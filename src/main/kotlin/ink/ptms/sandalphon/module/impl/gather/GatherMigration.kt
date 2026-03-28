package ink.ptms.sandalphon.module.impl.gather

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ink.ptms.sandalphon.module.impl.gather.data.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.getDataFolder
import taboolib.library.xseries.parseToMaterial
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 数据迁移: 旧 blockmine JSON -> 新 gather YAML
 *
 * 使用原始 JSON 解析，不依赖旧 blockmine 数据类。
 *
 * 映射关系:
 *   BlockData.id          -> GatherPoint.id
 *   BlockData.growTime     -> GatherPoint.growTime
 *   BlockData.growChange   -> GatherPoint.growChance
 *   BlockProgress          -> GatherStage
 *   BlockStructure         -> GatherBlock
 *   BlockDrop              -> GatherDrop
 *   BlockState             -> GatherInstance
 */
val GatherMigration = subCommand {
    execute<Player> { sender, _, _ ->
        doMigrate(sender)
    }
}

private fun doMigrate(sender: CommandSender) {
    val sourceDir = File(getDataFolder(), "module/blockmine")
    if (!sourceDir.exists() || !sourceDir.isDirectory) {
        sender.error("未找到旧数据目录: module/blockmine/")
        return
    }

    val jsonFiles = sourceDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
    if (jsonFiles.isEmpty()) {
        sender.error("module/blockmine/ 中没有 JSON 文件.")
        return
    }

    var pointCount = 0
    var instanceCount = 0
    var skippedCount = 0

    for (file in jsonFiles) {
        try {
            val root = JsonParser().parse(file.readText(StandardCharsets.UTF_8)).asJsonObject

            val id = if (root.has("id")) root.get("id").asString else file.nameWithoutExtension

            // 如果新系统已存在同名模板则跳过
            if (GatherManager.points.containsKey(id)) {
                sender.info("跳过 '$id': 新系统中已存在同名模板.")
                skippedCount++
                continue
            }

            val growTime = if (root.has("growTime")) root.get("growTime").asInt else 60
            val growChance = if (root.has("growChange")) root.get("growChange").asDouble else 1.0

            val point = GatherPoint(id)
            point.gatherType = GatherType.LEFT  // 旧系统只支持左键
            point.growTime = growTime
            point.growChance = growChance

            // 解析 progress 数组 -> stages
            point.stages.clear()
            var firstTool: String? = null

            if (root.has("progress") && root.get("progress").isJsonArray) {
                val progressArray = root.getAsJsonArray("progress")
                for (i in 0 until progressArray.size()) {
                    val progressObj = progressArray.get(i).asJsonObject
                    val stage = GatherStage()
                    val dropMap = linkedMapOf<String, GatherDrop>()

                    if (progressObj.has("structures") && progressObj.get("structures").isJsonArray) {
                        val structures = progressObj.getAsJsonArray("structures")
                        for (j in 0 until structures.size()) {
                            val struct = structures.get(j).asJsonObject

                            // 解析方块
                            val originStr = if (struct.has("origin")) struct.get("origin").asString else "STONE"
                            val replaceStr = if (struct.has("replace")) struct.get("replace").asString else "AIR"
                            val offsetStr = if (struct.has("offset")) struct.get("offset").asString else "0.0,0.0,0.0"
                            val dirStr = if (struct.has("direction")) struct.get("direction").asString else "NORTH"

                            val offsetParts = offsetStr.split(",")
                            val offsetX = offsetParts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                            val offsetY = offsetParts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                            val offsetZ = offsetParts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

                            stage.blocks.add(GatherBlock(
                                material = runCatching { originStr.parseToMaterial() }.getOrNull() ?: Material.STONE,
                                replace = runCatching { replaceStr.parseToMaterial() }.getOrNull() ?: Material.AIR,
                                offset = listOf(offsetX, offsetY, offsetZ),
                                direction = runCatching { BlockFace.valueOf(dirStr) }.getOrDefault(BlockFace.NORTH)
                            ))

                            // 提取工具
                            if (firstTool == null && struct.has("tool") && !struct.get("tool").isJsonNull) {
                                val tool = struct.get("tool").asString
                                if (tool.isNotBlank()) firstTool = tool
                            }

                            // 解析掉落物
                            if (struct.has("drop") && struct.get("drop").isJsonArray) {
                                val dropArray = struct.getAsJsonArray("drop")
                                for (k in 0 until dropArray.size()) {
                                    val dropObj = dropArray.get(k).asJsonObject
                                    if (!dropObj.has("item")) continue
                                    val item = dropObj.get("item").asString
                                    val amount = if (dropObj.has("amount")) dropObj.get("amount").asInt else 1
                                    val chance = if (dropObj.has("chance")) dropObj.get("chance").asDouble else 1.0
                                    val key = "$item:$amount:$chance"
                                    if (!dropMap.containsKey(key)) {
                                        dropMap[key] = GatherDrop(
                                            item = item,
                                            amount = amount,
                                            chance = chance
                                        )
                                    }
                                }
                            }
                        }
                    }

                    stage.drops.addAll(dropMap.values)
                    point.stages.add(stage)
                }
            }

            if (point.stages.isEmpty()) {
                point.stages.add(GatherStage())
            }

            if (firstTool != null) {
                point.tool = firstTool
                point.toolCheck = ToolCheckType.ID
            }

            // 注册到管理器
            GatherManager.points[point.id] = point

            // 解析实例 (blocks 数组)
            val instList = mutableListOf<GatherInstance>()
            if (root.has("blocks") && root.get("blocks").isJsonArray) {
                val blocksArray = root.getAsJsonArray("blocks")
                for (b in 0 until blocksArray.size()) {
                    val blockObj = blocksArray.get(b).asJsonObject
                    if (!blockObj.has("location")) continue
                    val locStr = blockObj.get("location").asString
                    val locParts = locStr.split(",")
                    val worldName = locParts.getOrNull(0) ?: continue
                    val x = locParts.getOrNull(1)?.toDoubleOrNull() ?: continue
                    val y = locParts.getOrNull(2)?.toDoubleOrNull() ?: continue
                    val z = locParts.getOrNull(3)?.toDoubleOrNull() ?: continue
                    val world = Bukkit.getWorld(worldName) ?: continue

                    val current = if (blockObj.has("current")) blockObj.get("current").asInt else 0
                    val latest = if (blockObj.has("latest")) blockObj.get("latest").asLong else System.currentTimeMillis()
                    val update = if (blockObj.has("update")) blockObj.get("update").asBoolean else false

                    val inst = GatherInstance(
                        pointId = point.id,
                        location = Location(world, x, y, z)
                    )
                    inst.currentStage = current.coerceIn(0, (point.stages.size - 1).coerceAtLeast(0))
                    inst.lastGrowTime = latest
                    inst.needGrow = update
                    instList.add(inst)
                    instanceCount++
                }
            }
            GatherManager.instances[point.id] = instList

            // 保存到新格式
            GatherManager.savePoint(point)
            GatherManager.savePointInstances(point)

            pointCount++
        } catch (e: Exception) {
            sender.error("迁移文件 '${file.name}' 失败: ${e.message}")
            e.printStackTrace()
        }
    }

    // 重建缓存和索引
    GatherManager.rebuildCache()

    sender.success("迁移完成! 模板: $pointCount, 实例: $instanceCount, 跳过: $skippedCount")
    sender.info("使用 §f/gather list§7 查看已迁移的模板.")
}
