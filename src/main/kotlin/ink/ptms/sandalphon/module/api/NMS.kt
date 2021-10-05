package ink.ptms.sandalphon.module.api

import org.bukkit.block.Block
import org.bukkit.entity.Player
import taboolib.module.nms.nmsProxy

/**
 * @author sky
 * @since 2020-05-30 17:00
 */
abstract class NMS {

    abstract fun sendBlockAction(player: Player, block: Block, a: Int, b: Int)

    abstract fun setBlockData(block: Block, data: Byte)

    companion object {

        val instance by lazy { nmsProxy<NMS>() }
    }
}