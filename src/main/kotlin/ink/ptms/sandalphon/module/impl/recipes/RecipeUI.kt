package ink.ptms.sandalphon.module.impl.recipes

import ink.ptms.sandalphon.module.impl.recipes.RecipeMatcher.Companion.toMatcher
import ink.ptms.sandalphon.util.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.*
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import taboolib.common.reflect.Reflex.Companion.getProperty
import taboolib.common5.Coerce
import taboolib.common5.util.parseMillis
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.TellrawJson
import taboolib.module.nms.inputSign
import taboolib.module.ui.ClickType
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Linked
import taboolib.platform.util.buildItem
import taboolib.platform.util.inventoryCenterSlots
import taboolib.platform.util.isAir
import taboolib.platform.util.isNotAir
import java.util.*

val craftSlots = arrayOf(2 to 'A', 3 to 'B', 4 to 'C', 11 to 'D', 12 to 'E', 13 to 'F', 20 to 'G', 21 to 'H', 22 to 'I').associate { it.second to it.first }

fun Player.openRecipe(type: RecipeType, recipe: Recipe? = null) {
    when (type) {
        RecipeType.CRAFT_TABLE -> openRecipeCraftTable(recipe)
        RecipeType.FURNACE -> openRecipeFurnace(RecipeType.FURNACE, recipe)
        RecipeType.BLASTING -> openRecipeFurnace(RecipeType.BLASTING, recipe)
        RecipeType.SMOKING -> openRecipeFurnace(RecipeType.SMOKING, recipe)
    }
}

fun Player.openRecipes(type: RecipeType? = null) {
    if (type == null) {
        openMenu<Basic>("配方编辑器") {
            rows(3)
            map("", " 1 2 3 4")
            RecipeType.values().forEach { recipe ->
                set(recipe.id, buildItem(recipe.material) {
                    name = "&f${recipe.display}配方"
                    lore += "&7${Recipes.recipeMap[recipe]!!.recipes.size} 组"
                    colored()
                })
            }
            onClick(lock = true) {
                openRecipes(RecipeType.values().firstOrNull { r -> r.id == it.slot } ?: return@onClick)
            }
        }
    } else {
        openMenu<Linked<Recipe>>("配方编辑器 (${type.display}配方)") {
            rows(6)
            slots(inventoryCenterSlots)
            elements { Recipes.recipeMap[type]!!.recipes.values.toList() }
            onGenerate { _, element, _, _ ->
                buildItem(element.result) {
                    lore += listOf("", "&a[左键: 编辑配方] &b[右键: 复制序号] &c[Q: 删除]")
                    colored()
                }
            }
            onClick { event, element ->
                if (event.clickType == ClickType.CLICK) {
                    when {
                        event.clickEvent().isLeftClick -> {
                            openRecipe(type, element)
                        }
                        event.clickEvent().isRightClick -> {
                            element.getProperty<NamespacedKey>("key")?.also { key ->
                                TellrawJson().append("§9§l§n$key").hoverText("点击复制!").suggestCommand(key.key).sendTo(adaptPlayer(this@openRecipes))
                                closeInventory()
                            }
                        }
                        event.clickEvent().click == org.bukkit.event.inventory.ClickType.DROP -> {
                            element.getProperty<NamespacedKey>("key")?.also { key ->
                                Recipes.recipeMap[type]!!.removeRecipe(key)
                                openRecipes(type)
                            }
                        }
                    }
                }
            }
            set(49, buildItem(type.material) { name = "§7新建配方" }) {
                openRecipe(type)
            }
            setNextPage(51) { _, hasNextPage ->
                if (hasNextPage) {
                    buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7下一页" }
                } else {
                    buildItem(XMaterial.ARROW) { name = "§7下一页" }
                }
            }
            setNextPage(47) { _, hasNextPage ->
                if (hasNextPage) {
                    buildItem(XMaterial.SPECTRAL_ARROW) { name = "§7上一页" }
                } else {
                    buildItem(XMaterial.ARROW) { name = "§7上一页" }
                }
            }
        }
    }
}

private fun Player.openRecipeCraftTable(recipe: Recipe? = null) {
    val recipeMap = Recipes.recipeMap[RecipeType.CRAFT_TABLE]
    var save = true
    var shaped = recipe == null || recipe is ShapedRecipe
    fun button(): ItemStack {
        return ItemBuilder(XMaterial.CRAFTING_TABLE).name("&f${if (shaped) "有序" else "无序"}配方").lore("", "&7[SHIFT + 右键配方配置变种]").colored().build()
    }

    val matcher = HashMap<Int, RecipeMatcher>()
    openMenu<Basic>("配方编辑器 (${if (recipe == null) "新建" else "编辑"})") {
        handLocked(false)
        rows(3)
        map("##   #@##", "##   1 ##", "##   #@##")
        set('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
        set('@', ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).name("&r").colored().build())
        set('1', button())
        onBuild { _, inv ->
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
        onClick {
            when (it.slot) {
                '#', '@' -> {
                    it.isCancelled = true
                }
                '1' -> {
                    shaped = !shaped
                    it.isCancelled = true
                    it.currentItem = button()
                }
            }
            if (it.clickType == ClickType.CLICK && it.clickEvent().isShiftClick && it.currentItem.isNotAir() && craftSlots.any { slot -> slot.value == it.rawSlot }) {
                it.isCancelled = true
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
                openMenu<Basic>("配方编辑器 (变种)") {
                    handLocked(false)
                    rows(4)
                    map("0", "", "", "12#######")
                    set('0', it.currentItem ?: ItemStack(Material.AIR))
                    set('1', build1())
                    set('2', build2())
                    set('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
                    onClick { sub ->
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
                    onClose(once = false) { _ ->
                        matcher[it.rawSlot] = RecipeMatcher(
                            (0 until 27).mapNotNull { slot -> it.inventory.getItem(slot) }.filter { item -> item.isNotAir() },
                            ignoreItemMeta,
                            ignoreData
                        )
                        submit(delay = 1) {
                            openInventory(it.inventory)
                            save = true
                        }
                    }
                }
            }
        }
        onClose(once = false) {
            if (!save) {
                return@onClose
            }
            val key = if (recipe == null)
                NamespacedKey.minecraft(UUID.randomUUID().toString().replace("-", ""))
            else {
                recipe.getProperty("key")!!
            }
            val result = it.inventory.getItem(15)
            // 注销配方并保存
            if (result.isAir()) {
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
                                val item = it.inventory.getItem(slot.value)
                                if (item.isNotAir()) {
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
                                val item = it.inventory.getItem(slot.value)
                                if (item.isNotAir()) {
                                    r.addIngredient(item!!.toMatcher())
                                }
                            }
                        }
                    })
                }
            }
            submit(delay = 1) { openRecipes(RecipeType.CRAFT_TABLE) }
        }
    }
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
    openMenu<Basic>("配方编辑器 (${if (recipe == null) "新建" else "编辑"})") {
        rows(3)
        map("#########", "@@@ 1 @@@", "#########")
        set('1', button())
        set('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
        set('@', ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).name("&r").colored().build())
        onBuild { _, inv ->
            if (cookingRecipe != null) {
                inv.setItem(12, cookingRecipe!!.input)
                inv.setItem(14, cookingRecipe!!.result)
            }
        }
        onClick { event ->
            when (event.slot) {
                '#' -> {
                    event.isCancelled = true
                }
                '1' -> {
                    event.isCancelled = true
                    if (event.clickType == ClickType.CLICK) {
                        if (event.clickEvent().isLeftClick) {
                            save = false
                            inputSign(arrayOf("", "", "在第一行写入时间")) {
                                cookingTime = (it[0].parseMillis() / 50).toInt()
                                event.inventory.setItem(13, button())
                                openInventory(event.inventory)
                                save = true
                            }
                        } else if (event.clickEvent().isRightClick) {
                            save = false
                            inputSign(arrayOf("", "", "在第一行写入经验")) {
                                experience = Coerce.toFloat(it[0])
                                event.inventory.setItem(13, button())
                                openInventory(event.inventory)
                                save = true
                            }
                        }
                    }
                }
            }
            if (event.clickType == ClickType.CLICK && event.clickEvent().isShiftClick && event.currentItem.isNotAir() && event.rawSlot == 12) {
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
                openMenu<Basic>("配方编辑器 (变种)") {
                    rows(4)
                    map("0", "", "", "12#######")
                    set('0', event.currentItem ?: ItemStack(Material.AIR))
                    set('1', build1())
                    set('2', build2())
                    set('#', ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&r").colored().build())
                    onClick(lock = true) { sub ->
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
                    onClose(once = false) {
                        recipeChoice = RecipeMatcher(
                            (0 until 27).mapNotNull { slot -> it.inventory.getItem(slot) }.filter { item -> item.isNotAir() },
                            ignoreItemMeta,
                            ignoreData
                        )
                        submit(delay = 1) {
                            openInventory(event.inventory)
                            save = true
                        }
                    }
                }
            }
        }
        onClose(once = false) { inv ->
            println("页面关闭")
            if (!save) {
                println("不保存")
                return@onClose
            }
            val key = if (recipe == null)
                NamespacedKey.minecraft(UUID.randomUUID().toString().replace("-", ""))
            else {
                recipe.getProperty("key")!!
            }
            val source = inv.inventory.getItem(12)
            val result = inv.inventory.getItem(14)
            // 注销配方并保存
            if (source.isAir() || result.isAir()) {
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
            submit(delay = 1) { openRecipes(type) }
        }
    }
}
