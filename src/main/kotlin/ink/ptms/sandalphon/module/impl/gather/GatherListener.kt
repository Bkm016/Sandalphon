package ink.ptms.sandalphon.module.impl.gather

import ink.ptms.adyeshach.core.event.AdyeshachEntityInteractEvent
import ink.ptms.sandalphon.Sandalphon
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.EventPriority as TabooPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.chat.colored
import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag
import ink.ptms.sandalphon.module.impl.gather.data.*
import ink.ptms.sandalphon.module.impl.gather.event.GatherBreakEvent
import ink.ptms.sandalphon.module.impl.gather.hook.GatherHologram
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

object GatherListener {

    // ==================== 左键破坏 ====================

    @SubscribeEvent(priority = TabooPriority.LOW, ignoreCancelled = true)
    fun onBlockBreak(e: BlockBreakEvent) {
        val inst = GatherManager.findByLocation(e.block.location) ?: return
        val point = GatherManager.points[inst.pointId] ?: return

        // 只有 LEFT 或 BOTH 模式才响应左键
        if (point.gatherType == GatherType.RIGHT) {
            e.isCancelled = true
            e.player.info("该采集点需要右键读条采集.")
            return
        }

        e.isCancelled = true

        val block = GatherManager.findBlock(inst, e.block.location) ?: return
        if (e.block.type != block.material) return

        // 工具检查
        if (!GatherManager.checkTool(e.player, point)) {
            e.player.error("你需要使用 &f${point.tool} &7来采集.")
            return
        }

        doGather(e.player, inst, point, block, e.block.location)
    }

    // ==================== 右键读条 ====================

    @SubscribeEvent
    fun onInteract(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) return
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = e.clickedBlock ?: return

        val inst = GatherManager.findByLocation(clickedBlock.location) ?: return
        val point = GatherManager.points[inst.pointId] ?: return

        // 只有 RIGHT 或 BOTH 模式才响应右键
        if (point.gatherType == GatherType.LEFT) return

        e.isCancelled = true

        // 工具检查
        if (!GatherManager.checkTool(e.player, point)) {
            e.player.error("你需要使用 &f${point.tool} &7来采集.")
            return
        }

        // 检查是否已在采集
        if (GatherManager.gatheringSessions.containsKey(e.player.uniqueId)) {
            e.player.error("你正在采集中...")
            return
        }

        // 开始读条
        startGathering(e.player, inst, point)
    }

    // ==================== NPC 右键交互 ====================

    @SubscribeEvent
    fun onNPCInteract(e: AdyeshachEntityInteractEvent) {
        val inst = GatherManager.findByNPC(e.entity.normalizeUniqueId) ?: return
        val point = GatherManager.points[inst.pointId] ?: return

        e.isCancelled = true

        if (!GatherManager.checkTool(e.player, point)) {
            e.player.error("你需要使用 &f${point.tool} &7来采集.")
            return
        }

        if (GatherManager.gatheringSessions.containsKey(e.player.uniqueId)) {
            e.player.error("你正在采集中...")
            return
        }

        startGathering(e.player, inst, point)
    }

    // ==================== 读条逻辑 ====================

    private fun startGathering(player: Player, inst: GatherInstance, point: GatherPoint) {
        val session = GatherSession(
            playerUUID = player.uniqueId,
            instance = inst,
            point = point,
            startTick = System.currentTimeMillis(),
            totalTicks = point.gatherTime
        )
        GatherManager.gatheringSessions[player.uniqueId] = session
    }

    @Schedule(period = 1)
    fun tickGathering() {
        val iterator = GatherManager.gatheringSessions.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, session) = iterator.next()
            val player = org.bukkit.Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline) {
                iterator.remove()
                continue
            }

            session.currentTick++

            // 显示进度条
            val progress = session.progress()
            val bar = createLoad(session.totalTicks.toDouble(), (session.totalTicks - session.currentTick).toDouble(), 20)
            val percent = (progress * 100).toInt()
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent("§e采集中 $bar §f${percent}%".colored()))

            // 粒子效果
            if (session.currentTick % 5 == 0) {
                val loc = session.instance.location.clone().add(0.5, 0.5, 0.5)
                player.world.spawnParticle(Particle.CRIT, loc, 3, 0.3, 0.3, 0.3, 0.0)
            }

            if (session.isComplete()) {
                iterator.remove()
                // 采集完成
                val point = session.point
                val inst = session.instance

                if (point.npcEnable) {
                    // NPC模式：直接处理掉落
                    doGatherNPC(player, inst, point)
                } else {
                    // 方块模式：处理当前阶段所有方块
                    val stage = point.stages.getOrNull(inst.currentStage) ?: continue
                    for (block in stage.blocks) {
                        val blockLoc = inst.location.clone().add(block.offsetX, block.offsetY, block.offsetZ)
                        doGather(player, inst, point, block, blockLoc)
                    }
                }
            }
        }
    }

    // ==================== 读条中断 ====================

    @SubscribeEvent
    fun onMove(e: PlayerMoveEvent) {
        if (e.from.blockX != e.to?.blockX || e.from.blockY != e.to?.blockY || e.from.blockZ != e.to?.blockZ) {
            cancelGathering(e.player, "移动导致采集中断.")
        }
    }

    @SubscribeEvent
    fun onSwitchItem(e: PlayerItemHeldEvent) {
        cancelGathering(e.player, "切换物品导致采集中断.")
    }

    @SubscribeEvent
    fun onDamage(e: EntityDamageEvent) {
        val player = e.entity as? Player ?: return
        cancelGathering(player, "受到攻击导致采集中断.")
    }

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        GatherManager.gatheringSessions.remove(e.player.uniqueId)
    }

    private fun cancelGathering(player: Player, reason: String) {
        if (GatherManager.gatheringSessions.remove(player.uniqueId) != null) {
            player.error(reason)
        }
    }

    // ==================== 采集执行 ====================

    /**
     * 方块模式采集执行
     */
    private fun doGather(player: Player, inst: GatherInstance, point: GatherPoint, block: GatherBlock, blockLoc: org.bukkit.Location) {
        val stageIndex = inst.currentStage

        // 触发自定义事件
        val event = GatherBreakEvent(player, point, inst, stageIndex)
        if (!event.call()) return

        // 替换方块
        blockLoc.block.type = block.replace

        // 破坏粒子
        blockLoc.world?.players?.filter { it != player }?.forEach {
            it.playEffect(blockLoc, Effect.STEP_SOUND, block.material)
        }

        // 掉落物
        val stage = point.stages.getOrNull(stageIndex) ?: return
        dropItems(player, stage, blockLoc)

        // 执行Kether
        executeKether(player, stage, point)

        // duration < 0: 采集后立刻恢复到阶段0
        if (stage.duration < 0) {
            inst.currentStage = 0
            inst.needGrow = false
            inst.lastGrowTime = System.currentTimeMillis()
            GatherManager.buildInstance(inst)
            GatherManager.reindexInstance(inst)
            GatherHologram.show(inst, point)
            return
        }

        // 标记需要生长
        inst.needGrow = true
        inst.lastGrowTime = System.currentTimeMillis()
        GatherManager.growQueue.add(inst)

        // 隐藏全息
        GatherHologram.hide(inst)
    }

    /**
     * NPC模式采集执行
     */
    private fun doGatherNPC(player: Player, inst: GatherInstance, point: GatherPoint) {
        val stageIndex = inst.currentStage

        val event = GatherBreakEvent(player, point, inst, stageIndex)
        if (!event.call()) return

        // 移除NPC
        inst.npcEntity?.remove()
        inst.npcUUID?.let { GatherManager.npcIndex.remove(it) }
        inst.npcEntity = null
        inst.npcUUID = null

        // 掉落物
        val stage = point.stages.getOrNull(stageIndex) ?: return
        dropItems(player, stage, inst.location.clone().add(0.5, 0.5, 0.5))

        // 执行Kether
        executeKether(player, stage, point)

        // duration < 0: 采集后立刻恢复到阶段0
        if (stage.duration < 0) {
            inst.currentStage = 0
            inst.needGrow = false
            inst.lastGrowTime = System.currentTimeMillis()
            if (point.npcEnable) {
                GatherManager.spawnNPC(inst, point)
            } else {
                GatherManager.buildInstance(inst)
                GatherManager.reindexInstance(inst)
            }
            GatherHologram.show(inst, point)
            return
        }

        // 标记需要生长
        inst.needGrow = true
        inst.lastGrowTime = System.currentTimeMillis()
        GatherManager.growQueue.add(inst)

        // 隐藏全息
        GatherHologram.hide(inst)
    }

    // ==================== 掉落物 ====================

    /** 掉落物实体 -> 拾取后执行的 Kether 动作 */
    val pickupActions = ConcurrentHashMap<Int, List<String>>()

    private fun dropItems(player: Player, stage: GatherStage, loc: org.bukkit.Location) {
        val dropLoc = loc.clone().add(0.5, 0.5, 0.5)
        for (drop in stage.drops) {
            if (ThreadLocalRandom.current().nextDouble() > drop.chance) continue
            // 优先尝试 Zaphkiel，失败则尝试原版 Material
            val isZaphkielItem = GatherManager.getItem(player, drop.item) != null
            val item = GatherManager.getItem(player, drop.item)
                ?: runCatching { org.bukkit.inventory.ItemStack(Material.valueOf(drop.item.uppercase())) }.getOrNull()
                ?: continue
            item.amount = drop.amount

            // 原版物品设置自定义名称
            if (!isZaphkielItem && drop.displayName.isNotEmpty()) {
                val meta = item.itemMeta
                if (meta != null) {
                    meta.setDisplayName(drop.displayName.replace("&", "§"))
                    item.itemMeta = meta
                }
            }

            // 如果有拾取动作，给物品打上 NBT 标记
            if (drop.onPickupActions.isNotEmpty()) {
                val tag = item.getItemTag()
                tag["GatherPickup"] = ItemTagData("true")
                item.setItemTag(tag)
            }

            val droppedEntity = loc.world?.dropItem(dropLoc, item) ?: continue
            droppedEntity.pickupDelay = 20

            // 缓存拾取动作（用实体ID做key，实体消失后自动失效）
            if (drop.onPickupActions.isNotEmpty()) {
                pickupActions[droppedEntity.entityId] = drop.onPickupActions.toList()
            }
        }
    }

    // ==================== 拾取监听 ====================

    @SubscribeEvent
    fun onPickupGatherDrop(e: EntityPickupItemEvent) {
        val player = e.entity as? Player ?: return
        val entityId = e.item.entityId
        val actions = pickupActions.remove(entityId) ?: return
        // 执行拾取 Kether 动作
        try {
            KetherShell.eval(actions, ScriptOptions.builder().apply {
                namespace(listOf("adyeshach", "adyeshach-inner", "chemdah", "taboo-public-work"))
                sender(adaptPlayer(player))
                vars(mapOf("player" to player, "item" to e.item.itemStack))
            }.build())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /** 定期清理已失效的缓存（实体已消失但未被拾取的情况） */
    @Schedule(period = 20 * 60)
    fun cleanPickupCache() {
        if (pickupActions.isEmpty()) return
        val validIds = org.bukkit.Bukkit.getWorlds().flatMap { it.entities }
            .filterIsInstance<Item>()
            .map { it.entityId }
            .toSet()
        pickupActions.keys.removeAll { it !in validIds }
    }

    // ==================== Kether ====================

    private fun executeKether(player: Player, stage: GatherStage, point: GatherPoint) {
        if (stage.ketherActions.isEmpty()) return
        try {
            KetherShell.eval(stage.ketherActions, ScriptOptions.builder().apply {
                namespace(listOf("adyeshach", "adyeshach-inner", "chemdah", "taboo-public-work"))
                sender(adaptPlayer(player))
                vars(mapOf("player" to player, "point" to point.id))
            }.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
