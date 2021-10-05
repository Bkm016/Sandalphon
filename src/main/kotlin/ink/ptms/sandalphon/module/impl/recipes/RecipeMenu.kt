package ink.ptms.sandalphon.module.impl.recipes

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.recipes.RecipeMatcher.Companion.toMatcher
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.internal.xseries.XMaterial
import io.izzel.taboolib.kotlin.Reflex.Companion.reflex
import io.izzel.taboolib.kotlin.Tasks
import io.izzel.taboolib.module.tellraw.TellrawJson
import io.izzel.taboolib.util.Coerce
import io.izzel.taboolib.util.Features
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.item.inventory.ClickEvent
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.item.inventory.linked.MenuLinked
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.*
import java.util.*

fun Player.openRecipes(type: RecipeType? = null) {
    if (type == null) {
        MenuBuilder.builder(Sandalphon.plugin)
            .title("配方编辑器")
            .rows(3)
            .items("", " 1 2 3 4")
            .also {
                RecipeType.values().forEach { recipe ->
                    it.put(
                        recipe.id, ItemBuilder(recipe.material)
                            .name("&f${recipe.display}配方")
                            .lore("&7${Recipes.recipeMap[recipe]!!.recipes.size} 组")
                            .colored()
                            .build()
                    )
                }
            }.click {
                openRecipes(RecipeType.values().firstOrNull { r -> r.id == it.slot } ?: return@click)
            }.open(this)
    } else {
        object : MenuLinked<Recipe>(this) {

            init {
                addButtonNextPage(51)
                addButtonPreviousPage(47)
                addButton(49) {
                    openRecipe(type)
                }
            }

            override fun getTitle(): String {
                return "配方编辑器 (${type.display}配方)"
            }

            override fun getRows(): Int {
                return 6
            }

            override fun getElements(): MutableList<Recipe> {
                return Recipes.recipeMap[type]!!.recipes.values.toMutableList()
            }

            override fun getSlots(): MutableList<Int> {
                return Items.INVENTORY_CENTER.toMutableList()
            }

            override fun onBuild(inventory: Inventory) {
                if (hasNextPage()) {
                    inventory.setItem(51, ItemBuilder(XMaterial.SPECTRAL_ARROW).name("&7下一页").colored().build())
                } else {
                    inventory.setItem(51, ItemBuilder(XMaterial.ARROW).name("&8下一页").colored().build())
                }
                if (hasPreviousPage()) {
                    inventory.setItem(47, ItemBuilder(XMaterial.SPECTRAL_ARROW).name("&7上一页").colored().build())
                } else {
                    inventory.setItem(47, ItemBuilder(XMaterial.ARROW).name("&8上一页").colored().build())
                }
                inventory.setItem(49, ItemBuilder(type.material).name("&7新建配方").colored().build())
            }

            override fun onClick(event: ClickEvent, element: Recipe) {
                if (event.clickType == ClickType.CLICK) {
                    when {
                        event.castClick().isLeftClick -> {
                            player.openRecipe(type, element)
                        }
                        event.castClick().isRightClick -> {
                            element.reflex<NamespacedKey>("key")?.also { key ->
                                TellrawJson.create().append("§9§l§n$key").hoverText("Click To Copy!").clickSuggest(key.key).send(player)
                                player.closeInventory()
                            }
                        }
                        event.castClick().click == org.bukkit.event.inventory.ClickType.DROP -> {
                            element.reflex<NamespacedKey>("key")?.also { key ->
                                Recipes.recipeMap[type]!!.removeRecipe(key)
                                open(page)
                            }
                        }
                    }
                }
            }

            override fun generateItem(player: Player, element: Recipe, index: Int, slot: Int): ItemStack {
                return ItemBuilder(element.result).lore("", "&a[左键: 编辑配方] &b[右键: 复制序号] &c[Q: 删除]").colored().build()
            }
        }.open()
    }
}

fun Player.openRecipe(type: RecipeType, recipe: Recipe? = null) {
    when (type) {
        RecipeType.CRAFT_TABLE -> openRecipeCraftTable(recipe)
        RecipeType.FURNACE -> openRecipeFurnace(RecipeType.FURNACE, recipe)
        RecipeType.BLASTING -> openRecipeFurnace(RecipeType.BLASTING, recipe)
        RecipeType.SMOKING -> openRecipeFurnace(RecipeType.SMOKING, recipe)
    }
}

private val craftSlots = arrayOf(
    2 to 'A',
    3 to 'B',
    4 to 'C',
    11 to 'D',
    12 to 'E',
    13 to 'F',
    20 to 'G',
    21 to 'H',
    22 to 'I'
).map { it.second to it.first }.toMap()

private fun Player.openRecipeCraftTable(recipe: Recipe? = null) {
    val recipeMap = Recipes.recipeMap[RecipeType.CRAFT_TABLE]
    var save = true
    var shaped = recipe == null || recipe is ShapedRecipe
    fun button(): ItemStack {
        return ItemBuilder(XMaterial.CRAFTING_TABLE).name("&f${if (shaped) "有序" else "无序"}配方").lore("", "&7[SHIFT + 右键配方配置变种]").colored().build()
    }

    val matcher = HashMap<Int, RecipeMatcher>()
    MenuBuilder.builder(Sandalphon.plugin)
        .title("配方编辑器 (${if (recipe == null) "新建" else "编辑"})")
        .rows(3)
        .items(
            "##   #@##",
            "##   1 ##",
            "##   #@##"
        )
        .put('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
        .put('@', ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).name("&r").colored().build())
        .put('1', button())
        .build { inv ->
            when (recipe) {
                is ShapedRecipe -> {
                    inv.setItem(15, recipe.result)
                    recipe.ingredientMap.forEach { (k, v) ->
                        inv.setItem(craftSlots[k]!!, v)
                    }
                }
                is ShapelessRecipe -> {
                    inv.setItem(15, recipe.result)
                    recipe.ingredientList.forEachIndexed { index, v ->
                        inv.setItem(craftSlots[craftSlots.keys.toList()[index]]!!, v)
                    }
                }
            }
        }
        .event { event ->
            when (event.slot) {
                '#', '@' -> {
                    event.isCancelled = true
                }
                '1' -> {
                    shaped = !shaped
                    event.isCancelled = true
                    event.currentItem = button()
                }
            }
            if (event.clickType == ClickType.CLICK
                && event.castClick().isShiftClick
                && Items.nonNull(event.currentItem)
                && craftSlots.any { slot -> slot.value == event.rawSlot }
            ) {
                event.isCancelled = true
                var ignoreItemMeta = false
                var ignoreData = false
                fun build1(): ItemStack {
                    return ItemBuilder(if (ignoreItemMeta) XMaterial.RED_TERRACOTTA else XMaterial.GREEN_TERRACOTTA)
                        .name(if (ignoreItemMeta) "&c忽略元数据" else "&a匹配元数据")
                        .colored()
                        .build()
                }

                fun build2(): ItemStack {
                    return ItemBuilder(if (ignoreData) XMaterial.RED_TERRACOTTA else XMaterial.GREEN_TERRACOTTA)
                        .name(if (ignoreData) "&c忽略 Zaphkiel 数据" else "&a匹配 Zaphkiel 数据")
                        .colored()
                        .build()
                }
                save = false
                MenuBuilder.builder(Sandalphon.plugin)
                    .title("配方编辑器 (变种)")
                    .rows(4)
                    .items("0", "", "", "12#######")
                    .put('0', event.currentItem)
                    .put('1', build1())
                    .put('2', build2())
                    .put('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
                    .event { sub ->
                        when (sub.slot) {
                            '#', '0' -> {
                                sub.isCancelled = true
                            }
                            '1' -> {
                                ignoreItemMeta = !ignoreItemMeta
                                sub.isCancelled = true
                                sub.currentItem = build1()
                            }
                            '2' -> {
                                ignoreData = !ignoreData
                                sub.isCancelled = true
                                sub.currentItem = build2()
                            }
                        }
                    }
                    .close {
                        matcher[event.rawSlot] = RecipeMatcher(
                            (0 until 27).mapNotNull { slot -> it.inventory.getItem(slot) }.filter { item -> Items.nonNull(item) },
                            ignoreItemMeta,
                            ignoreData
                        )
                        Tasks.delay(1) {
                            openInventory(event.inventory)
                            save = true
                        }
                    }.open(this)
            }
        }.close { inv ->
            if (!save) {
                return@close
            }
            val key = if (recipe == null)
                NamespacedKey.minecraft(UUID.randomUUID().toString().replace("-", ""))
            else {
                recipe.reflex("key")!!
            }
            val result = inv.inventory.getItem(15)
            // 注销配方并保存
            if (Items.isNull(result)) {
                recipeMap!!.removeRecipe(key)
            } else {
                recipeMap!!.removeRecipe(key, false)
                if (shaped) {
                    recipeMap.addRecipe(ShapedRecipe(key, result!!).also { r ->
                        r.shape("ABC", "DEF", "GHI")
                        r.group = result.hashCode().toString()
                        craftSlots.forEach { slot ->
                            if (matcher.containsKey(slot.value)) {
                                r.setIngredient(slot.key, matcher[slot.value]!!)
                            } else {
                                val item = inv.inventory.getItem(slot.value)
                                if (Items.nonNull(item)) {
                                    r.setIngredient(slot.key, item!!.toMatcher())
                                }
                            }
                        }
                    })
                } else {
                    recipeMap.addRecipe(ShapelessRecipe(key, result!!).also { r ->
                        r.group = result.hashCode().toString()
                        craftSlots.forEach { slot ->
                            if (matcher.containsKey(slot.value)) {
                                r.addIngredient(matcher[slot.value]!!)
                            } else {
                                val item = inv.inventory.getItem(slot.value)
                                if (Items.nonNull(item)) {
                                    r.addIngredient(item!!.toMatcher())
                                }
                            }
                        }
                    })
                }
            }
            Tasks.delay(1) {
                openRecipes(RecipeType.CRAFT_TABLE)
            }
        }.open(this)
}

private fun Player.openRecipeFurnace(type: RecipeType, recipe: Recipe? = null) {
    val recipeMap = Recipes.recipeMap[type]
    var save = true
    var recipeChoice: RecipeMatcher? = null
    var cookingRecipe = recipe as? CookingRecipe<*>
    var cookingTime = cookingRecipe?.cookingTime ?: 200
    var experience = cookingRecipe?.experience ?: 0f
    fun button(): ItemStack {
        return ItemBuilder(type.material).name("&f${type.display}配方 (${cookingTime / 20}s) (${experience} EXP)")
            .lore("", "&a[左键: 燃烧时间]", "&e[右键: 产出经验]", "&7[SHIFT + 右键配方配置变种]")
            .colored()
            .build()
    }
    MenuBuilder.builder(Sandalphon.plugin)
        .title("配方编辑器 (${if (recipe == null) "新建" else "编辑"})")
        .rows(3)
        .items(
            "#########",
            "@@@ 1 @@@",
            "#########"
        )
        .put('1', button())
        .put('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
        .put('@', ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).name("&r").colored().build())
        .build { inv ->
            if (cookingRecipe != null) {
                inv.setItem(12, cookingRecipe!!.input)
                inv.setItem(14, cookingRecipe!!.result)
            }
        }
        .event { event ->
            when (event.slot) {
                '#' -> {
                    event.isCancelled = true
                }
                '1' -> {
                    event.isCancelled = true
                    if (event.clickType == ClickType.CLICK) {
                        if (event.castClick().isLeftClick) {
                            save = false
                            Features.inputSign(player, arrayOf("", "", "在第一行写入时间")) {
                                cookingTime = (CronusUtils.toMillis(it[0]) / 50).toInt()
                                event.inventory.setItem(13, button())
                                openInventory(event.inventory)
                                save = true
                            }
                        } else if (event.castClick().isRightClick) {
                            save = false
                            Features.inputSign(player, arrayOf("", "", "在第一行写入经验")) {
                                experience = Coerce.toFloat(it[0])
                                event.inventory.setItem(13, button())
                                openInventory(event.inventory)
                                save = true
                            }
                        }
                    }
                }
            }
            if (event.clickType == ClickType.CLICK
                && event.castClick().isShiftClick
                && Items.nonNull(event.currentItem)
                && event.rawSlot == 12
            ) {
                event.isCancelled = true
                var ignoreItemMeta = false
                var ignoreData = false
                fun build1(): ItemStack {
                    return ItemBuilder(if (ignoreItemMeta) XMaterial.RED_TERRACOTTA else XMaterial.GREEN_TERRACOTTA)
                        .name(if (ignoreItemMeta) "&c忽略元数据" else "&a匹配元数据")
                        .colored()
                        .build()
                }

                fun build2(): ItemStack {
                    return ItemBuilder(if (ignoreData) XMaterial.RED_TERRACOTTA else XMaterial.GREEN_TERRACOTTA)
                        .name(if (ignoreData) "&c忽略 Zaphkiel 数据" else "&a匹配 Zaphkiel 数据")
                        .colored()
                        .build()
                }
                save = false
                MenuBuilder.builder(Sandalphon.plugin)
                    .title("配方编辑器 (变种)")
                    .rows(4)
                    .items("0", "", "", "12#######")
                    .put('0', event.currentItem)
                    .put('1', build1())
                    .put('2', build2())
                    .put('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
                    .event { sub ->
                        when (sub.slot) {
                            '#', '0' -> {
                                sub.isCancelled = true
                            }
                            '1' -> {
                                ignoreItemMeta = !ignoreItemMeta
                                sub.isCancelled = true
                                sub.currentItem = build1()
                            }
                            '2' -> {
                                ignoreData = !ignoreData
                                sub.isCancelled = true
                                sub.currentItem = build2()
                            }
                        }
                    }
                    .close {
                        recipeChoice = RecipeMatcher(
                            (0 until 27).mapNotNull { slot -> it.inventory.getItem(slot) }.filter { item -> Items.nonNull(item) },
                            ignoreItemMeta,
                            ignoreData
                        )
                        Tasks.delay(1) {
                            openInventory(event.inventory)
                            save = true
                        }
                    }.open(this)
            }
        }.close { inv ->
            if (!save) {
                return@close
            }
            val key = if (recipe == null)
                NamespacedKey.minecraft(UUID.randomUUID().toString().replace("-", ""))
            else {
                recipe.reflex("key")!!
            }
            val source = inv.inventory.getItem(12)
            val result = inv.inventory.getItem(14)
            // 注销配方并保存
            if (Items.isNull(source) || Items.isNull(result)) {
                recipeMap!!.removeRecipe(key)
            } else {
                recipeMap!!.removeRecipe(key, false)
                cookingRecipe = when (type) {
                    RecipeType.FURNACE -> FurnaceRecipe(key, result!!, recipeChoice ?: source!!.toMatcher(), experience, cookingTime)
                    RecipeType.BLASTING -> BlastingRecipe(key, result!!, recipeChoice ?: source!!.toMatcher(), experience, cookingTime)
                    RecipeType.SMOKING -> SmokingRecipe(key, result!!, recipeChoice ?: source!!.toMatcher(), experience, cookingTime)
                    else -> null
                }
                if (cookingRecipe != null) {
                    recipeMap.addRecipe(cookingRecipe!!)
                }
            }
            Tasks.delay(1) {
                openRecipes(type)
            }
        }.open(this)
}
