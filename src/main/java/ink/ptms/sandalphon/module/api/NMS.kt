package ink.ptms.sandalphon.module.api

import io.izzel.taboolib.module.inject.TInject
import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * @author sky
 * @since 2020-05-30 17:00
 */
abstract class NMS {
    abstract fun sendBlockAction(player: Player?, block: Block?, a: Int, b: Int)
    abstract fun setBlockData(block: Block?, data: Byte)

    companion object {
        @TInject(asm = "ink.ptms.sandalphon.module.api.NMSHandle")
        val HANDLE: NMS? = null
    }
}