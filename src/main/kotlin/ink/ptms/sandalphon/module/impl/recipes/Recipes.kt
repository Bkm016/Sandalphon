package ink.ptms.sandalphon.module.impl.recipes

import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.library.xseries.getItemStack

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipe.Recipes
 *
 * @author sky
 * @since 2021/3/15 12:04 上午
 */
object Recipes {

    val recipeMap = HashMap<RecipeType, RecipeMap>()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        RecipeType.values().forEach { recipeMap[it] = RecipeMap(it) }
    }

    @Suppress("UNCHECKED_CAST")
    @Awake(LifeCycle.ACTIVE)
    fun import() {
        cancel()
        recipeMap[RecipeType.CRAFT_TABLE]?.also { map ->
            map.load()
            map.data.getConfigurationSection("craft")?.also { craft ->
                craft.getKeys(false).forEach { key ->
                    val namespacedKey = NamespacedKey.minecraft(key.substring(key.indexOf(":") + 1, key.length))
                    when (craft.getString("$key.type")) {
                        "shape" -> {
                            map.addRecipe(ShapedRecipe(namespacedKey, craft.getItemStack("$key.result")!!).also { shapedRecipe ->
                                shapedRecipe.shape("ABC", "DEF", "GHI")
                                shapedRecipe.group = shapedRecipe.result.hashCode().toString()
                                craft.getConfigurationSection("$key.ingredient")?.getKeys(false)?.forEach { i ->
                                    shapedRecipe.setIngredient(
                                        i.toCharArray()[0], RecipeMatcher(
                                            craft.getList("$key.ingredient.$i.items") as List<ItemStack>,
                                            craft.getBoolean("$key.ingredient.$i.ignoreItemMeta"),
                                            craft.getBoolean("$key.ingredient.$i.ignoreData")
                                        )
                                    )
                                }
                            }, false)
                        }
                        "shapeless" -> {
                            map.addRecipe(ShapelessRecipe(namespacedKey, craft.getItemStack("$key.result")!!).also { shapelessRecipe ->
                                shapelessRecipe.group = shapelessRecipe.result.hashCode().toString()
                                craft.getConfigurationSection("$key.ingredient")?.getKeys(false)?.forEach { i ->
                                    shapelessRecipe.addIngredient(
                                        RecipeMatcher(
                                            craft.getList("$key.ingredient.$i.items") as List<ItemStack>,
                                            craft.getBoolean("$key.ingredient.$i.ignoreItemMeta"),
                                            craft.getBoolean("$key.ingredient.$i.ignoreData")
                                        )
                                    )
                                }
                            }, false)
                        }
                    }
                }
            }
        }
        fun importFurnace(type: RecipeType) {
            recipeMap[type]?.also { map ->
                map.load()
                map.data.getConfigurationSection("furnace")?.also { furnace ->
                    furnace.getKeys(false).forEach { key ->
                        val namespacedKey = NamespacedKey.minecraft(key.substring(key.indexOf(":") + 1, key.length))
                        val result = furnace.getItemStack("$key.result")!!
                        val choice = RecipeMatcher(
                            furnace.getList("$key.input.items") as List<ItemStack>,
                            furnace.getBoolean("$key.input.ignoreItemMeta"),
                            furnace.getBoolean("$key.input.ignoreData")
                        )
                        val experience = furnace.getDouble("$key.experience").toFloat()
                        val cookingTime = furnace.getInt("$key.cookingTime")
                        val cookingRecipe = when (type) {
                            RecipeType.FURNACE -> FurnaceRecipe(namespacedKey, result, choice, experience, cookingTime)
                            RecipeType.BLASTING -> BlastingRecipe(namespacedKey, result, choice, experience, cookingTime)
                            RecipeType.SMOKING -> SmokingRecipe(namespacedKey, result, choice, experience, cookingTime)
                            else -> null
                        }
                        if (cookingRecipe != null) {
                            map.addRecipe(cookingRecipe, false)
                        }
                    }
                }
            }
        }
        importFurnace(RecipeType.FURNACE)
        importFurnace(RecipeType.BLASTING)
        importFurnace(RecipeType.SMOKING)
    }

    @Awake(LifeCycle.DISABLE)
    fun cancel() {
        recipeMap.forEach { (_, v) -> v.clearRecipes() }
    }

    fun export() {
        recipeMap[RecipeType.CRAFT_TABLE]?.also { map ->
            map.data.set("craft", null)
            map.data.set("craft", map.data.createSection("craft").also { craft ->
                map.recipes.forEach { (key, recipe) ->
                    craft.set("$key.result", recipe.result)
                    when (recipe) {
                        is ShapedRecipe -> {
                            craft.set("$key.type", "shape")
                            recipe.choiceMap.forEach { (k, v) ->
                                if (v is RecipeMatcher) {
                                    craft.set("$key.ingredient.$k.items", v.items)
                                    craft.set("$key.ingredient.$k.ignoreItemMeta", v.ignoreItemMeta)
                                    craft.set("$key.ingredient.$k.ignoreData", v.ignoreData)
                                }
                            }
                        }
                        is ShapelessRecipe -> {
                            craft.set("$key.type", "shapeless")
                            recipe.choiceList.forEachIndexed { k, v ->
                                if (v is RecipeMatcher) {
                                    craft.set("$key.ingredient.$k.items", v.items)
                                    craft.set("$key.ingredient.$k.ignoreItemMeta", v.ignoreItemMeta)
                                    craft.set("$key.ingredient.$k.ignoreData", v.ignoreData)
                                }
                            }
                        }
                    }
                }
            })
            map.save()
        }
        fun exportFurnace(type: RecipeType) {
            recipeMap[type]?.also { map ->
                map.data.set("furnace", null)
                map.data.set("furnace", map.data.createSection("furnace").also { furnace ->
                    map.recipes.forEach { (key, recipe) ->
                        if (recipe is CookingRecipe<*>) {
                            val input = recipe.inputChoice
                            if (input is RecipeMatcher) {
                                furnace.set("$key.result", recipe.result)
                                furnace.set("$key.experience", recipe.experience)
                                furnace.set("$key.cookingTime", recipe.cookingTime)
                                furnace.set("$key.input.items", input.items)
                                furnace.set("$key.input.ignoreItemMeta", input.ignoreItemMeta)
                                furnace.set("$key.input.ignoreData", input.ignoreData)
                            }
                        }
                    }
                })
                map.save()
            }
        }
        exportFurnace(RecipeType.FURNACE)
        exportFurnace(RecipeType.BLASTING)
        exportFurnace(RecipeType.SMOKING)
    }
}