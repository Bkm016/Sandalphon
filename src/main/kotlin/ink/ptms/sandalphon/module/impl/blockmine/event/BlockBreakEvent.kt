package ink.ptms.sandalphon.module.impl.blockmine.event

import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockStructure
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * @author sky
 * @since 2020-06-02 13:59
 */
class BlockBreakEvent(
    val player: Player,
    val blockData: BlockData,
    val blockState: BlockState,
    val blockStructure: BlockStructure,
    val bukkitEvent: org.bukkit.event.block.BlockBreakEvent
) : BukkitProxyEvent()