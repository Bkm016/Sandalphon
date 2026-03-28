package ink.ptms.sandalphon.module.impl.gather.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent
import ink.ptms.sandalphon.module.impl.gather.data.GatherInstance
import ink.ptms.sandalphon.module.impl.gather.data.GatherPoint

/**
 * 采集完成事件（可取消）
 */
class GatherBreakEvent(
    val player: Player,
    val point: GatherPoint,
    val instance: GatherInstance,
    val stageIndex: Int
) : BukkitProxyEvent() {

    override val allowCancelled = true
}
