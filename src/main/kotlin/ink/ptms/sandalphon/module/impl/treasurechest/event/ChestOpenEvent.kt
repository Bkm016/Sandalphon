package ink.ptms.sandalphon.module.impl.treasurechest.event

import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * @author sky
 * @since 2020-05-30 16:35
 */
class ChestOpenEvent(val player: Player, val chestData: ChestData) : BukkitProxyEvent()