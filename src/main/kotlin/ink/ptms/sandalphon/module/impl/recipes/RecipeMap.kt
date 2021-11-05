package ink.ptms.sandalphon.module.impl.recipes

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.Recipe
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.common.reflect.Reflex.Companion.getProperty

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipes.RecipeMap
 *
 * @author sky
 * @since 2021/3/15 6:48 上午
 */
class RecipeMap(val type: RecipeType) {

    val file by lazy { newFile(getDataFolder(), "module/recipes/$type.yml") }

    val data by lazy {
        val yaml = YamlConfiguration()
        yaml.load(file)
        yaml
    }

    val recipes = HashMap<NamespacedKey, Recipe>()

    fun addRecipe(recipe: Recipe, export: Boolean = true) {
        recipes[recipe.getProperty("key")!!] = recipe
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
        data.load(file)
    }

    fun save() {
        data.save(file)
    }

    enum class Type {

        CRAFT_TABLE, FURNACE, SMOKING, BLASTING
    }
}