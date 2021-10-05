package ink.ptms.sandalphon.module.impl.recipes

import ink.ptms.sandalphon.Sandalphon
import io.izzel.taboolib.kotlin.Reflex.Companion.reflex
import io.izzel.taboolib.module.db.local.SecuredFile
import io.izzel.taboolib.util.Files
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipes.RecipeMap
 *
 * @author sky
 * @since 2021/3/15 6:48 上午
 */
class RecipeMap(val type: RecipeType) {

    val file = Files.file(Sandalphon.plugin.dataFolder, "module/recipes/$type.yml")
    val conf = SecuredFile.loadConfiguration(file)!!

    val recipes = HashMap<NamespacedKey, Recipe>()

    fun addRecipe(recipe: Recipe, export: Boolean = true) {
        recipes[recipe.reflex("key")!!] = recipe
        Bukkit.addRecipe(recipe)
        if (export) {
            Recipes.export()
        }
    }

    fun removeRecipe(key: NamespacedKey, export: Boolean = true) {
        recipes.remove(key)
        Bukkit.removeRecipe(key)
        if (export) {
            Bukkit.getOnlinePlayers().forEach {
                it.undiscoverRecipe(key)
            }
            Recipes.export()
        }
    }

    fun clearRecipes() {
        recipes.forEach { r ->
            Bukkit.removeRecipe(r.key)
            Bukkit.getOnlinePlayers().forEach {
                it.undiscoverRecipe(r.key)
            }
        }
        recipes.clear()
    }

    fun load() {
        conf.load(file)
    }

    fun save() {
        conf.save(file)
    }

    enum class Type {

        CRAFT_TABLE, FURNACE, SMOKING, BLASTING
    }
}