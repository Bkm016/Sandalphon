package ink.ptms.sandalphon.module.impl.treasurechest.data

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/**
 * @author sky
 * @since 2020-05-30 17:49
 */
class ChestInventory(val chestData: ChestData) : InventoryHolder {

    override fun getInventory(): Inventory = Bukkit.createInventory(null, 9)
}