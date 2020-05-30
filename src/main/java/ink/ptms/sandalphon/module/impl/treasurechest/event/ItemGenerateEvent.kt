package ink.ptms.sandalphon.module.impl.treasurechest.event

import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.zaphkiel.api.ItemStream
import io.izzel.taboolib.module.event.EventCancellable
import io.izzel.taboolib.module.event.EventNormal
import org.bukkit.entity.Player

/**
 * @Author sky
 * @Since 2020-05-30 16:35
 */
class ItemGenerateEvent(val player: Player, val chestData: ChestData, var item: ItemStream, var amount: Int) : EventCancellable<ItemGenerateEvent>()