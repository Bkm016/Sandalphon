package ink.ptms.sandalphon.module.impl.gather.event

import taboolib.platform.type.BukkitProxyEvent
import ink.ptms.sandalphon.module.impl.gather.data.GatherInstance
import ink.ptms.sandalphon.module.impl.gather.data.GatherPoint

/**
 * 方块恢复/生长事件（可取消）
 */
class GatherGrowEvent(
    val point: GatherPoint,
    val instance: GatherInstance
) : BukkitProxyEvent() {

    override val allowCancelled = true
}
