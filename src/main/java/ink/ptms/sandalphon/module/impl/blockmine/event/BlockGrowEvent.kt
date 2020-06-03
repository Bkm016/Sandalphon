package ink.ptms.sandalphon.module.impl.blockmine.event

import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import io.izzel.taboolib.module.event.EventCancellable
import io.izzel.taboolib.module.event.EventNormal

/**
 * @Author sky
 * @Since 2020-06-02 13:59
 */
class BlockGrowEvent(val blockData: BlockData, val blockState: BlockState) : EventCancellable<BlockGrowEvent>() {
}