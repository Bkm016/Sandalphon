package ink.ptms.sandalphon

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Sandalphon
 * ink.ptms.sandalphon.ItemAPI
 *
 * @author 坏黑
 * @since 2022/12/1 18:13
 */
interface ItemAPI {

    /** 获取物品 ID */
    fun getId(itemStack: ItemStack): String?

    /** 获取物品 */
    fun getItem(id: String, player: Player?): ItemStack?

    /** 获取物品数据 */
    fun getData(itemStack: ItemStack, node: String): String?

    /** 获取物品数据列表 */
    fun getDataList(itemStack: ItemStack, node: String): List<String>
}