package ink.ptms.sandalphon.module.impl.gather

import org.bukkit.event.player.PlayerEditBookEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.chat.uncolored

/**
 * 监听书本编辑事件，处理 Gather 的 Kether 动作和拾取动作编辑
 *
 * 书本 lore 格式:
 *   [0] = "Gather"
 *   [1] = pointId
 *   [2] = "kether" 或 "pickup"
 *   [3] = stageIndex
 *   [4] = dropIndex (仅 pickup 模式)
 */
object GatherBookListener {

    @SubscribeEvent
    fun onEditBook(e: PlayerEditBookEvent) {
        if (!e.player.isOp) return
        val meta = e.previousBookMeta
        val lore = meta.lore ?: return
        if (lore.isEmpty()) return
        if (lore[0].uncolored() != "Gather") return

        val pointId = lore.getOrNull(1)?.uncolored() ?: return
        val type = lore.getOrNull(2)?.uncolored() ?: return
        val stageIndex = lore.getOrNull(3)?.uncolored()?.toIntOrNull() ?: return

        val point = GatherManager.points[pointId]
        if (point == null) {
            e.player.error("采集模板 '$pointId' 已失效.")
            return
        }

        val stage = point.stages.getOrNull(stageIndex)
        if (stage == null) {
            e.player.error("阶段 $stageIndex 不存在.")
            return
        }

        // 解析书页内容为动作列表
        val pages = e.newBookMeta.pages
        val isClear = pages.isNotEmpty() && pages[0].uncolored().trim() == "clear"
        val actions = if (isClear) {
            mutableListOf()
        } else {
            pages.flatMap { page ->
                page.replace("§0", "").uncolored().split("\n")
            }.filter { it.isNotBlank() }.toMutableList()
        }

        when (type) {
            "kether" -> {
                stage.ketherActions.clear()
                stage.ketherActions.addAll(actions)
                e.player.success("Kether动作已更新, 共 ${actions.size} 条.")
            }
            "pickup" -> {
                val dropIndex = lore.getOrNull(4)?.uncolored()?.toIntOrNull() ?: return
                val drop = stage.drops.getOrNull(dropIndex)
                if (drop == null) {
                    e.player.error("掉落物 $dropIndex 不存在.")
                    return
                }
                drop.onPickupActions.clear()
                drop.onPickupActions.addAll(actions)
                e.player.success("拾取动作已更新, 共 ${actions.size} 条.")
            }
        }

        e.isSigning = false
        GatherManager.saveInstances()
    }
}
