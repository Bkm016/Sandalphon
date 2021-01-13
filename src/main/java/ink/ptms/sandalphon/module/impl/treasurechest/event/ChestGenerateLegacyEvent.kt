package ink.ptms.sandalphon.module.impl.treasurechest.event

import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import io.izzel.taboolib.module.event.EventCancellable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @Author sky
 * @Since 2020-05-30 16:35
 */
class ChestGenerateLegacyEvent(val player: Player, val chestData: ChestData, var item: ItemStack, var amount: Int) : EventCancellable<ChestGenerateLegacyEvent>()