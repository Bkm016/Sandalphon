package ink.ptms.sandalphon.module.impl.gather

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityTypes
import ink.ptms.adyeshach.core.entity.manager.ManagerType
import ink.ptms.adyeshach.impl.entity.controller.ControllerLookAtPlayer
import ink.ptms.sandalphon.Sandalphon
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.io.newFile
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import ink.ptms.sandalphon.module.impl.gather.data.*
import ink.ptms.sandalphon.module.impl.gather.event.GatherGrowEvent
import ink.ptms.sandalphon.module.impl.gather.hook.GatherHologram
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GatherManager {

    /** 采集点模板 id -> GatherPoint */
    val points = ConcurrentHashMap<String, GatherPoint>()

    /** 实例位置索引 locationKey -> GatherInstance */
    val locationIndex = ConcurrentHashMap<String, GatherInstance>()

    /** 所有实例列表（按模板ID分组） */
    val instances = ConcurrentHashMap<String, MutableList<GatherInstance>>()

    /** 需要生长的实例队列 */
    val growQueue = ConcurrentHashMap.newKeySet<GatherInstance>()

    /** NPC UUID -> GatherInstance 映射 */
    val npcIndex = ConcurrentHashMap<UUID, GatherInstance>()

    /** 正在采集的玩家 UUID -> 采集会话 */
    val gatheringSessions = ConcurrentHashMap<UUID, GatherSession>()

    /** 材质缓存（快速排除无关方块） */
    val materialCache = ConcurrentHashMap.newKeySet<Material>()

    /** 数据根目录: data/gather/impl/ */
    private fun implDir(): File = File(getDataFolder(), "data/gather/impl")

    /** 获取某个采集点的目录 */
    private fun pointDir(id: String): File = File(implDir(), id)

    // ==================== 生命周期 ====================

    @Awake(LifeCycle.ENABLE)
    fun load() {
        reload()
    }

    @Awake(LifeCycle.DISABLE)
    fun save() {
        saveAll()
        GatherHologram.clearAll()
        npcIndex.values.forEach { inst ->
            inst.npcEntity?.remove()
        }
    }

    fun reload() {
        GatherHologram.clearAll()
        npcIndex.values.forEach { it.npcEntity?.remove() }
        points.clear()
        locationIndex.clear()
        instances.clear()
        growQueue.clear()
        npcIndex.clear()
        materialCache.clear()

        // 从 resources 加载预设模板
        loadPointsFromResources()
        // 从 impl 文件夹加载（覆盖同名预设）
        loadPointsFromImpl()
        // 重建缓存
        rebuildCache()
        // 重建全息和NPC
        rebuildVisuals()

        info("[Gather] 加载了 ${points.size} 个采集模板, ${locationIndex.size} 个实例")
    }

    // ==================== 模板加载 ====================

    private fun loadPointsFromResources() {
        val targetDir = File(getDataFolder(), "gather/points")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
            releaseResourceFile("gather/points/example.yml", replace = false)
        }
        targetDir.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val config = Configuration.loadFromFile(file, Type.YAML)
            config.getKeys(false).forEach { key ->
                val section = config.getConfigurationSection(key) ?: return@forEach
                try {
                    val point = parsePoint(key, section)
                    points[point.id] = point
                } catch (e: Exception) {
                    warning("[Gather] 加载预设模板 '$key' 失败: ${e.message}")
                }
            }
        }
    }

    private fun loadPointsFromImpl() {
        val dir = implDir()
        if (!dir.exists()) return
        dir.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
            val pointFile = File(folder, "point.yml")
            if (pointFile.exists()) {
                try {
                    val config = Configuration.loadFromFile(pointFile, Type.YAML)
                    val point = parsePoint(folder.name, config)
                    points[point.id] = point
                } catch (e: Exception) {
                    warning("[Gather] 加载模板 '${folder.name}/point.yml' 失败: ${e.message}")
                }
            }
            // 加载该采集点的实例
            val instFile = File(folder, "instances.yml")
            if (instFile.exists()) {
                try {
                    val config = Configuration.loadFromFile(instFile, Type.YAML)
                    loadInstancesFromConfig(folder.name, config)
                } catch (e: Exception) {
                    warning("[Gather] 加载实例 '${folder.name}/instances.yml' 失败: ${e.message}")
                }
            }
        }
    }

    private fun loadInstancesFromConfig(pointId: String, config: Configuration) {
        config.getKeys(false).forEach { key ->
            try {
                val section = config.getConfigurationSection(key) ?: return@forEach
                val world = Bukkit.getWorld(section.getString("world") ?: return@forEach) ?: return@forEach
                val x = section.getDouble("x")
                val y = section.getDouble("y")
                val z = section.getDouble("z")
                val loc = Location(world, x, y, z)

                val inst = GatherInstance(pointId, loc)
                inst.currentStage = section.getInt("currentStage", 0)
                inst.lastGrowTime = section.getLong("lastGrowTime", 0L)
                inst.needGrow = section.getBoolean("needGrow", false)

                if (section.contains("hologramOffset")) {
                    inst.hologramOffset = section.getDoubleList("hologramOffset")
                }
                if (section.contains("hologramEnable")) {
                    inst.hologramEnable = section.getBoolean("hologramEnable")
                }

                registerInstance(inst)
                if (inst.needGrow) {
                    growQueue.add(inst)
                }
            } catch (e: Exception) {
                warning("[Gather] 加载实例 '$pointId/$key' 失败: ${e.message}")
            }
        }
    }

    fun parsePoint(id: String, section: ConfigurationSection): GatherPoint {
        val point = GatherPoint(section.getString("id") ?: id)
        point.gatherType = runCatching {
            GatherType.valueOf(section.getString("gather-type", "RIGHT")!!.uppercase())
        }.getOrDefault(GatherType.RIGHT)
        point.gatherTime = section.getInt("gather-time", 40)
        point.tool = section.getString("tool", "")!!
        point.toolCheck = runCatching {
            ToolCheckType.valueOf(section.getString("tool-check", "NONE")!!.uppercase())
        }.getOrDefault(ToolCheckType.NONE)
        point.growTime = section.getInt("grow.time", 60)
        point.growChance = section.getDouble("grow.chance", 1.0)

        point.hologramEnable = section.getBoolean("hologram.enable", false)
        point.hologramOffset = section.getDoubleList("hologram.offset").ifEmpty { listOf(0.0, 0.0, 0.0) }
        point.hologramLines = section.getStringList("hologram.lines").toMutableList()

        point.npcEnable = section.getBoolean("npc.enable", false)
        point.npcType = runCatching {
            EntityTypes.valueOf(section.getString("npc.type", "VILLAGER")!!.uppercase())
        }.getOrDefault(EntityTypes.VILLAGER)
        point.npcName = section.getString("npc.name", "")!!
        point.npcLookAtPlayer = section.getBoolean("npc.look-at-player", true)

        val stageList = section.getMapList("stages")
        for (stageMap in stageList) {
            val stage = GatherStage()
            @Suppress("UNCHECKED_CAST")
            val blocksList = stageMap["blocks"] as? List<Map<String, Any>> ?: emptyList()
            for (blockMap in blocksList) {
                val gb = GatherBlock()
                gb.material = runCatching { Material.valueOf(blockMap["material"].toString().uppercase()) }.getOrDefault(Material.STONE)
                gb.replace = runCatching { Material.valueOf(blockMap["replace"].toString().uppercase()) }.getOrDefault(Material.AIR)
                @Suppress("UNCHECKED_CAST")
                gb.offset = (blockMap["offset"] as? List<Number>)?.map { it.toDouble() } ?: listOf(0.0, 0.0, 0.0)
                gb.direction = runCatching { BlockFace.valueOf(blockMap["direction"].toString().uppercase()) }.getOrDefault(BlockFace.NORTH)
                stage.blocks.add(gb)
            }
            @Suppress("UNCHECKED_CAST")
            val dropsList = stageMap["drops"] as? List<Map<String, Any>> ?: emptyList()
            for (dropMap in dropsList) {
                val drop = GatherDrop()
                drop.item = dropMap["item"]?.toString() ?: ""
                drop.amount = (dropMap["amount"] as? Number)?.toInt() ?: 1
                drop.chance = (dropMap["chance"] as? Number)?.toDouble() ?: 1.0
                drop.displayName = dropMap["display-name"]?.toString() ?: ""
                @Suppress("UNCHECKED_CAST")
                drop.onPickupActions.addAll((dropMap["on-pickup-actions"] as? List<String>) ?: emptyList())
                stage.drops.add(drop)
            }
            @Suppress("UNCHECKED_CAST")
            stage.ketherActions.addAll((stageMap["kether-actions"] as? List<String>) ?: emptyList())
            stage.duration = (stageMap["duration"] as? Number)?.toInt() ?: 0
            point.stages.add(stage)
        }

        return point
    }

    // ==================== 持久化保存 ====================

    /**
     * 保存所有数据（模板+实例），每个采集点一个文件夹
     */
    fun saveAll() {
        points.values.forEach { point ->
            savePoint(point)
            savePointInstances(point)
        }
    }

    /**
     * 保存单个采集点模板 -> impl/{id}/point.yml
     */
    fun savePoint(point: GatherPoint) {
        val file = newFile(pointDir(point.id), "point.yml", create = true)
        val config = Configuration.loadFromFile(file, Type.YAML)

        // 清空旧数据
        config.getKeys(false).forEach { config[it] = null }

        config["id"] = point.id
        config["gather-type"] = point.gatherType.name
        config["gather-time"] = point.gatherTime
        config["tool"] = point.tool
        config["tool-check"] = point.toolCheck.name
        config["grow.time"] = point.growTime
        config["grow.chance"] = point.growChance
        config["hologram.enable"] = point.hologramEnable
        config["hologram.offset"] = point.hologramOffset
        config["hologram.lines"] = point.hologramLines
        config["npc.enable"] = point.npcEnable
        config["npc.type"] = point.npcType.name
        config["npc.name"] = point.npcName
        config["npc.look-at-player"] = point.npcLookAtPlayer

        val stagesList = mutableListOf<Map<String, Any>>()
        point.stages.forEach { stage ->
            val stageMap = mutableMapOf<String, Any>()
            val blocksList = mutableListOf<Map<String, Any>>()
            stage.blocks.forEach { block ->
                blocksList.add(mapOf(
                    "material" to block.material.name,
                    "replace" to block.replace.name,
                    "offset" to block.offset,
                    "direction" to block.direction.name
                ))
            }
            stageMap["blocks"] = blocksList
            val dropsList = mutableListOf<Map<String, Any>>()
            stage.drops.forEach { drop ->
                val dropMap = mutableMapOf<String, Any>(
                    "item" to drop.item,
                    "amount" to drop.amount,
                    "chance" to drop.chance,
                    "display-name" to drop.displayName
                )
                if (drop.onPickupActions.isNotEmpty()) {
                    dropMap["on-pickup-actions"] = drop.onPickupActions.toList()
                }
                dropsList.add(dropMap)
            }
            stageMap["drops"] = dropsList
            if (stage.ketherActions.isNotEmpty()) {
                stageMap["kether-actions"] = stage.ketherActions.toList()
            }
            stageMap["duration"] = stage.duration
            stagesList.add(stageMap)
        }
        config["stages"] = stagesList
        config.saveToFile()
    }

    /**
     * 保存单个采集点的实例 -> impl/{id}/instances.yml
     */
    fun savePointInstances(point: GatherPoint) {
        val file = newFile(pointDir(point.id), "instances.yml", create = true)
        val config = Configuration.loadFromFile(file, Type.YAML)

        config.getKeys(false).forEach { config[it] = null }

        val instList = instances[point.id] ?: emptyList()
        instList.forEachIndexed { index, inst ->
            val path = "i$index"
            config["$path.world"] = inst.location.world?.name
            config["$path.x"] = inst.location.x
            config["$path.y"] = inst.location.y
            config["$path.z"] = inst.location.z
            config["$path.currentStage"] = inst.currentStage
            config["$path.lastGrowTime"] = inst.lastGrowTime
            config["$path.needGrow"] = inst.needGrow
            inst.hologramOffset?.let { config["$path.hologramOffset"] = it }
            inst.hologramEnable?.let { config["$path.hologramEnable"] = it }
        }
        config.saveToFile()
    }

    /**
     * 兼容旧调用：保存所有
     */
    fun saveInstances() {
        saveAll()
    }

    // ==================== 实例管理 ====================

    fun registerInstance(inst: GatherInstance) {
        val point = points[inst.pointId] ?: return
        indexInstance(inst, point)
        instances.computeIfAbsent(inst.pointId) { mutableListOf() }.add(inst)
    }

    fun unregisterInstance(inst: GatherInstance) {
        val point = points[inst.pointId]
        if (point != null) {
            for (stage in point.stages) {
                for (block in stage.blocks) {
                    val blockLoc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
                    locationIndex.remove(GatherInstance.toLocationKey(blockLoc))
                }
            }
        }
        instances[inst.pointId]?.remove(inst)
        growQueue.remove(inst)
        inst.npcEntity?.remove()
        inst.npcUUID?.let { npcIndex.remove(it) }
        GatherHologram.remove(inst)
    }

    private fun indexInstance(inst: GatherInstance, point: GatherPoint) {
        if (!point.npcEnable) {
            val stage = point.stages.getOrNull(inst.currentStage) ?: return
            for (block in stage.blocks) {
                val blockLoc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
                locationIndex[GatherInstance.toLocationKey(blockLoc)] = inst
            }
        }
    }

    fun reindexInstance(inst: GatherInstance) {
        val point = points[inst.pointId] ?: return
        for (stage in point.stages) {
            for (block in stage.blocks) {
                val blockLoc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
                val key = GatherInstance.toLocationKey(blockLoc)
                if (locationIndex[key] == inst) {
                    locationIndex.remove(key)
                }
            }
        }
        indexInstance(inst, point)
    }

    // ==================== 缓存 ====================

    fun rebuildCache() {
        materialCache.clear()
        points.values.forEach { point ->
            point.stages.forEach { stage ->
                stage.blocks.forEach { block ->
                    materialCache.add(block.material)
                    materialCache.add(block.replace)
                }
            }
        }
    }

    // ==================== 查找 ====================

    fun findByLocation(loc: Location): GatherInstance? {
        if (loc.block.type !in materialCache) return null
        return locationIndex[GatherInstance.toLocationKey(loc)]
    }

    fun findByNPC(uuid: UUID): GatherInstance? {
        return npcIndex[uuid]
    }

    fun findBlock(inst: GatherInstance, loc: Location): GatherBlock? {
        val point = points[inst.pointId] ?: return null
        val stage = point.stages.getOrNull(inst.currentStage) ?: return null
        for (block in stage.blocks) {
            val blockLoc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
            if (blockLoc.blockX == loc.blockX && blockLoc.blockY == loc.blockY && blockLoc.blockZ == loc.blockZ
                && blockLoc.world?.name == loc.world?.name
            ) {
                return block
            }
        }
        return null
    }

    // ==================== 方块操作 ====================

    fun buildInstance(inst: GatherInstance) {
        val point = points[inst.pointId] ?: return
        val stage = point.stages.getOrNull(inst.currentStage) ?: return
        for (block in stage.blocks) {
            val loc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
            val bukkitBlock = loc.block
            if (bukkitBlock.type == block.material) continue
            bukkitBlock.type = block.material
            val blockData = bukkitBlock.blockData
            if (blockData is Directional) {
                blockData.facing = block.direction
                bukkitBlock.blockData = blockData
            }
            loc.world?.spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0.0)
        }
    }

    fun cleanInstance(inst: GatherInstance) {
        val point = points[inst.pointId] ?: return
        val stage = point.stages.getOrNull(inst.currentStage) ?: return
        for (block in stage.blocks) {
            val loc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
            loc.block.type = block.replace
        }
    }

    fun isBroken(inst: GatherInstance): Boolean {
        val point = points[inst.pointId] ?: return false
        val stage = point.stages.getOrNull(inst.currentStage) ?: return false
        return stage.blocks.all { block ->
            val loc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
            loc.block.type != block.material
        }
    }

    // ==================== 生长调度 ====================

    @Schedule(period = 20)
    fun tickGrow() {
        val now = System.currentTimeMillis()

        // 1. 处理被破坏后的恢复（growQueue）
        if (growQueue.isNotEmpty()) {
            val iterator = growQueue.iterator()
            while (iterator.hasNext()) {
                val inst = iterator.next()
                val point = points[inst.pointId] ?: run {
                    iterator.remove()
                    continue
                }
                if (now - inst.lastGrowTime < point.growTime * 1000L) continue
                if (point.growChance < 1.0 && Math.random() > point.growChance) {
                    inst.lastGrowTime = now
                    continue
                }
                if (!GatherGrowEvent(point, inst).call()) continue

                if (point.npcEnable) {
                    spawnNPC(inst, point)
                } else {
                    if (isBroken(inst)) {
                        inst.currentStage = 0
                    } else {
                        cleanInstance(inst)
                        inst.currentStage = if (inst.currentStage + 1 >= point.stages.size) 0 else inst.currentStage + 1
                    }
                    buildInstance(inst)
                    reindexInstance(inst)
                }

                inst.needGrow = false
                inst.lastGrowTime = now
                iterator.remove()
                GatherHologram.show(inst, point)
            }
        }

        // 2. 处理阶段定时推进（duration > 0 的阶段到时间自动切换）
        instances.values.flatten().forEach { inst ->
            if (inst.needGrow) return@forEach // 被破坏的不参与定时推进
            val point = points[inst.pointId] ?: return@forEach
            val stage = point.stages.getOrNull(inst.currentStage) ?: return@forEach
            if (stage.duration <= 0) return@forEach // duration=0 不自动推进
            if (now - inst.lastGrowTime < stage.duration * 1000L) return@forEach

            if (!GatherGrowEvent(point, inst).call()) return@forEach

            if (!point.npcEnable) {
                cleanInstance(inst)
            }
            inst.currentStage = if (inst.currentStage + 1 >= point.stages.size) 0 else inst.currentStage + 1
            inst.lastGrowTime = now

            if (point.npcEnable) {
                // NPC模式下可以切换NPC外观等
                inst.npcEntity?.remove()
                spawnNPC(inst, point)
            } else {
                buildInstance(inst)
                reindexInstance(inst)
            }
            GatherHologram.show(inst, point)
        }
    }

    @Schedule(period = 20 * 60 * 5, async = true)
    fun autoSave() {
        saveAll()
    }

    // ==================== 视觉重建 ====================

    private fun rebuildVisuals() {
        instances.values.flatten().forEach { inst ->
            val point = points[inst.pointId] ?: return@forEach
            if (point.npcEnable) {
                if (!inst.needGrow) spawnNPC(inst, point)
            } else {
                if (!inst.needGrow) buildInstance(inst)
            }
            if (!inst.needGrow) GatherHologram.create(inst, point)
        }
    }

    // ==================== NPC 管理 ====================

    fun spawnNPC(inst: GatherInstance, point: GatherPoint) {
        inst.npcEntity?.remove()
        inst.npcUUID?.let { npcIndex.remove(it) }
        try {
            val manager = Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY)
            val npc = manager.create(point.npcType, inst.location.clone().add(0.5, 0.0, 0.5)) {
                it.setCustomName(point.npcName)
                it.setCustomNameVisible(point.npcName.isNotEmpty())
                it.setNoGravity(true)
                if (point.npcLookAtPlayer) {
                    it.registerController(ControllerLookAtPlayer(it, 8.0, 1.0, false, 100))
                }
            }
            inst.npcEntity = npc
            inst.npcUUID = npc.normalizeUniqueId
            npcIndex[npc.normalizeUniqueId] = inst
        } catch (e: Exception) {
            warning("[Gather] 创建NPC失败: ${e.message}")
        }
    }

    // ==================== 物品工具 (Zaphkiel) ====================

    fun getItem(player: Player, itemId: String): ItemStack? {
        return Sandalphon.itemAPI?.getItem(itemId, player)
    }

    fun getItemId(item: ItemStack): String? {
        return Sandalphon.itemAPI?.getId(item)
    }

    fun checkTool(player: Player, point: GatherPoint): Boolean {
        if (point.tool.isEmpty() || point.toolCheck == ToolCheckType.NONE) return true
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return false
        return when (point.toolCheck) {
            ToolCheckType.ID -> getItemId(item) == point.tool
            ToolCheckType.LORE -> {
                val lore = item.itemMeta?.lore ?: return false
                lore.any { it.contains(point.tool) }
            }
            ToolCheckType.NONE -> true
        }
    }
}

/**
 * 右键读条采集会话
 */
data class GatherSession(
    val playerUUID: UUID,
    val instance: GatherInstance,
    val point: GatherPoint,
    val startTick: Long,
    val totalTicks: Int,
    var currentTick: Int = 0
) {
    fun isComplete(): Boolean = currentTick >= totalTicks
    fun progress(): Double = currentTick.toDouble() / totalTicks.toDouble()
}
