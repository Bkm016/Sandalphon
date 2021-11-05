package ink.ptms.sandalphon.module.impl.recipes

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipes.RecipeMatcher
 *
 * @author sky
 * @since 2021/3/15 1:47 下午
 */
data class RecipeMatcher(val items: List<ItemStack>, val ignoreItemMeta: Boolean = false, val ignoreData: Boolean = false) : RecipeChoice.ExactChoice(items) {

    override fun test(item: ItemStack): Boolean {
        if (item.isAir()) {
            return false
        }
        // 忽略元数据
        if (ignoreItemMeta) {
            return items.any { it.type == item.type }
        }
        val test = item.clone()
        val compound = test.getItemTag()
        val zaphkiel = compound["zaphkiel"]
        if (zaphkiel != null) {
            // 忽略 Zaphkiel Data
            if (ignoreData) {
                zaphkiel.asCompound().remove("c")
            }
            zaphkiel.asCompound().remove("d")
            compound.saveTo(test)
        }
        return items.any { it.isSimilar(test) }
    }

    companion object {

        fun ItemStack.toMatcher() = RecipeMatcher(listOf(this))
    }
}