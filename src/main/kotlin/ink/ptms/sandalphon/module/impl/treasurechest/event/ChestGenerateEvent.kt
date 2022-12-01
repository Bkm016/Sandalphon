package ink.ptms.sandalphon.module.impl.treasurechest.event

import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

/**
 * @author sky
 * @since 2020-05-30 16:35
 */
class ChestGenerateEvent(val player: Player, val chestData: ChestData, var item: ItemStack, var amount: Int) : BukkitProxyEvent()