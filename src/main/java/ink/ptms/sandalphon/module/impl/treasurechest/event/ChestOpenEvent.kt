package ink.ptms.sandalphon.module.impl.treasurechest.event

import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import io.izzel.taboolib.module.event.EventCancellable
import org.bukkit.entity.Player

/**
 * @Author sky
 * @Since 2020-05-30 16:35
 */
class ChestOpenEvent(val player: Player, val chestData: ChestData) : EventCancellable<ChestOpenEvent>()