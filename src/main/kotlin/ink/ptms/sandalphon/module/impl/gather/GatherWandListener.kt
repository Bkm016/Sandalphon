package ink.ptms.sandalphon.module.impl.gather

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.platform.event.EventPriority as TabooPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.chat.uncolored
import taboolib.platform.util.hasLore
import taboolib.platform.util.hasName
import ink.ptms.sandalphon.module.impl.gather.data.GatherBlock
import ink.ptms.sandalphon.module.impl.gather.data.GatherInstance
import ink.ptms.sandalphon.module.impl.gather.data.GatherStage
import ink.ptms.sandalphon.module.impl.gather.hook.GatherHologram
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * 魔杖系统事件监听（场景魔杖、调试魔杖、捕获魔杖）
 */
object GatherWandListener {

    /** 捕获魔杖的选区缓存 */
    val captureSelection = ConcurrentHashMap<String, Pair<Location?, Location?>>()

    // ==================== 场景魔杖 左键放置 ====================

    @SubscribeEvent(priority = TabooPriority.LOWEST)
    fun onWandBreak(e: BlockBreakEvent) {
        if (!e.player.isOp) return
        val item = e.player.inventory.itemInMainHand
        if (!item.hasName("场景魔杖") || !item.hasLore("Gather")) return

        e.isCancelled = true
        val pointId = item.itemMeta?.lore?.getOrNull(1)?.uncolored() ?: return
        val point = GatherManager.points[pointId]
        if (point == null) {
            e.player.error("该魔杖已失效, 模板 '$pointId' 不存在.")
            return
        }

        // 检查位置是否已有实例
        if (GatherManager.findByLocation(e.block.location) != null) {
            e.player.error("该位置已存在采集点实例.")
            return
        }

        val loc = if (e.player.isSneaking) {
            e.block.location.add(0.0, 1.0, 0.0)
        } else {
            e.block.location.clone()
        }

        val inst = GatherInstance(pointId, loc)
        inst.currentStage = if (point.stages.isNotEmpty()) point.stages.size - 1 else 0

        GatherManager.registerInstance(inst)
        GatherManager.buildInstance(inst)
        GatherHologram.create(inst, point)

        if (point.npcEnable) {
            GatherManager.spawnNPC(inst, point)
        }

        GatherManager.saveInstances()
        e.player.info("采集点实例已创建. (${if (e.player.isSneaking) "上移1格" else "当前位置"})")
    }

    // ==================== 场景魔杖 右键删除 / 调试魔杖 右键切换 / 捕获魔杖 右键终点 ====================

    @SubscribeEvent
    fun onWandInteract(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) return
        if (!e.player.isOp) return
        val item = e.player.inventory.itemInMainHand

        // 场景魔杖 - 右键删除
        if (item.hasName("场景魔杖") && item.hasLore("Gather") && e.action == Action.RIGHT_CLICK_BLOCK) {
            e.isCancelled = true
            val pointId = item.itemMeta?.lore?.getOrNull(1)?.uncolored() ?: return
            val point = GatherManager.points[pointId]
            if (point == null) {
                e.player.error("该魔杖已失效.")
                return
            }
            val inst = GatherManager.findByLocation(e.clickedBlock!!.location)
            if (inst == null || inst.pointId != pointId) {
                e.player.error("该位置不存在此模板的实例.")
                return
            }
            GatherManager.cleanInstance(inst)
            GatherManager.unregisterInstance(inst)
            GatherManager.saveInstances()
            e.player.info("采集点实例已删除.")
            return
        }

        // 调试魔杖 - 右键切换阶段
        if (item.hasName("调试魔杖") && item.hasLore("Gather") && e.action == Action.RIGHT_CLICK_BLOCK) {
            e.isCancelled = true
            val pointId = item.itemMeta?.lore?.getOrNull(1)?.uncolored() ?: return
            val point = GatherManager.points[pointId]
            if (point == null) {
                e.player.error("该魔杖已失效.")
                return
            }
            val inst = GatherManager.findByLocation(e.clickedBlock!!.location)
            if (inst == null || inst.pointId != pointId) {
                e.player.error("该位置不存在此模板的实例.")
                return
            }
            GatherManager.cleanInstance(inst)
            inst.currentStage = if (inst.currentStage + 1 >= point.stages.size) 0 else inst.currentStage + 1
            GatherManager.buildInstance(inst)
            GatherManager.reindexInstance(inst)
            GatherManager.saveInstances()
            e.player.info("阶段已切换至 ${inst.currentStage}.")
            return
        }

        // 捕获魔杖 - 右键选终点
        if (item.hasName("捕获魔杖") && item.hasLore("Gather") && e.action == Action.RIGHT_CLICK_BLOCK) {
            e.isCancelled = true
            val pair = captureSelection.computeIfAbsent(e.player.name) { Pair(null, null) }
            captureSelection[e.player.name] = pair.copy(second = e.clickedBlock!!.location)
            // 粒子标记
            val loc = e.clickedBlock!!.location.clone().add(0.5, 0.5, 0.5)
            e.player.world.spawnParticle(Particle.FLAME, loc, 5, 0.1, 0.1, 0.1, 0.0)
            e.player.info("终点已选择.")
            return
        }
    }

    // ==================== 调试魔杖 左键重建 / 捕获魔杖 左键起点 ====================

    @SubscribeEvent(priority = TabooPriority.LOWEST)
    fun onDebugBreak(e: BlockBreakEvent) {
        if (!e.player.isOp) return
        val item = e.player.inventory.itemInMainHand

        // 调试魔杖 - 左键重建
        if (item.hasName("调试魔杖") && item.hasLore("Gather")) {
            e.isCancelled = true
            val pointId = item.itemMeta?.lore?.getOrNull(1)?.uncolored() ?: return
            val point = GatherManager.points[pointId]
            if (point == null) {
                e.player.error("该魔杖已失效.")
                return
            }
            val inst = GatherManager.findByLocation(e.block.location)
            if (inst == null || inst.pointId != pointId) {
                e.player.error("该位置不存在此模板的实例.")
                return
            }
            GatherManager.buildInstance(inst)
            e.player.info("实例已重建.")
            return
        }

        // 捕获魔杖 - 左键选起点
        if (item.hasName("捕获魔杖") && item.hasLore("Gather")) {
            e.isCancelled = true
            val pair = captureSelection.computeIfAbsent(e.player.name) { Pair(null, null) }
            captureSelection[e.player.name] = pair.copy(first = e.block.location)
            val loc = e.block.location.clone().add(0.5, 0.5, 0.5)
            e.player.world.spawnParticle(Particle.FLAME, loc, 5, 0.1, 0.1, 0.1, 0.0)
            e.player.info("起点已选择.")
            return
        }
    }

    // ==================== 捕获魔杖 丢弃完成捕获 ====================

    @SubscribeEvent
    fun onWandDrop(e: PlayerDropItemEvent) {
        if (!e.player.isOp) return
        val item = e.itemDrop.itemStack
        if (!item.hasName("捕获魔杖") || !item.hasLore("Gather")) return

        e.isCancelled = true
        val args = item.itemMeta?.lore?.getOrNull(1)?.uncolored()?.split(" ") ?: return
        if (args.size < 2) return

        val pointId = args[0]
        val stageIdx = args[1].toIntOrNull() ?: return
        val point = GatherManager.points[pointId]
        if (point == null) {
            e.player.error("该魔杖已失效.")
            return
        }
        val stage = point.stages.getOrNull(stageIdx)
        if (stage == null) {
            e.player.error("阶段索引无效.")
            return
        }

        val selection = captureSelection.remove(e.player.name)
        if (selection == null || selection.first == null || selection.second == null) {
            e.player.error("起点或终点缺失.")
            return
        }

        val locA = selection.first!!
        val locB = selection.second!!

        // 计算中心点作为锚点
        val mid = locA.toVector().midpoint(locB.toVector()).toLocation(e.player.world).run {
            this.y = min(locA.y, locB.y)
            this.block.location
        }

        // 清空旧结构
        stage.blocks.clear()

        // 遍历区域内所有方块
        val minX = min(locA.blockX, locB.blockX)
        val maxX = max(locA.blockX, locB.blockX)
        val minY = min(locA.blockY, locB.blockY)
        val maxY = max(locA.blockY, locB.blockY)
        val minZ = min(locA.blockZ, locB.blockZ)
        val maxZ = max(locA.blockZ, locB.blockZ)

        var count = 0
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val loc = Location(e.player.world, x.toDouble(), y.toDouble(), z.toDouble())
                    val block = loc.block
                    if (block.type == Material.AIR || block.type == Material.BEDROCK) continue

                    val direction = try {
                        val data = block.blockData
                        if (data is org.bukkit.block.data.Directional) data.facing else BlockFace.NORTH
                    } catch (_: Exception) {
                        BlockFace.NORTH
                    }

                    val offset = listOf(
                        loc.x - mid.x,
                        loc.y - mid.y,
                        loc.z - mid.z
                    )

                    stage.blocks.add(GatherBlock(
                        material = block.type,
                        replace = Material.AIR,
                        offset = offset,
                        direction = direction
                    ))

                    // 粒子标记
                    e.player.world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.0)
                    count++
                }
            }
        }

        GatherManager.rebuildCache()
        GatherManager.saveInstances()
        e.player.info("结构已捕获, 共 $count 个方块.")

        // 打开编辑界面
        GatherEditUI.openMainEdit(e.player, point)
    }
}
