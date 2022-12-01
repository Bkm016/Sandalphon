package ink.ptms.sandalphon

import ink.ptms.zaphkiel.Zaphkiel
import ink.ptms.zaphkiel.impl.item.toExtensionStreamOrNull
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Sandalphon
 * ink.ptms.sandalphon.ItemAPIImpl
 *
 * @author 坏黑
 * @since 2022/12/1 18:16
 */
class ItemAPIImpl : ItemAPI {

    override fun getId(itemStack: ItemStack): String? {
        return itemStack.toExtensionStreamOrNull()?.getZaphkielId()
    }

    override fun getItem(id: String, player: Player?): ItemStack? {
        return Zaphkiel.api().getItemManager().generateItemStack(id, player)
    }

    override fun getData(itemStack: ItemStack, node: String): String? {
        val itemStream = itemStack.toExtensionStreamOrNull() ?: return null
        return itemStream.getZaphkielData().getDeep(node)?.asString()
    }

    override fun getDataList(itemStack: ItemStack, node: String): List<String> {
        val itemStream = itemStack.toExtensionStreamOrNull() ?: return emptyList()
        return itemStream.getZaphkielData().getDeep(node)?.asList()?.map { it.asString() } ?: emptyList()
    }
}