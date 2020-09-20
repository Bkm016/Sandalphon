package ink.ptms.sandalphon.module.impl.holographic.data

import com.google.common.collect.Lists
import ink.ptms.cronus.internal.condition.Condition
import ink.ptms.cronus.internal.condition.ConditionParser
import ink.ptms.cronus.internal.program.NoneProgram
import ink.ptms.cronus.uranus.function.FunctionParser
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.hologram.Hologram
import io.izzel.taboolib.module.hologram.THologram
import io.izzel.taboolib.module.locale.TLocale
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Materials
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class HologramData(val id: String, var location: Location, var holoContent: MutableList<String> = ArrayList(), var holoCondition: MutableList<String> = ArrayList()) {

    val holograms = HashMap<String, List<Hologram>>()
    val condition = ArrayList<Condition>()

    var toFunction = false

    init {
        init()
    }

    fun init() {
        cancel()
        Bukkit.getOnlinePlayers().forEach { create(it) }
        condition.clear()
        condition.addAll(holoCondition.map { ConditionParser.parse(it) })
        toFunction = holoContent.any { FunctionParser.hasFunction(it) }
    }

    fun check(player: Player): Boolean {
        return condition.all { it.check(player) }
    }

    fun cancel() {
        holograms.forEach { it.value.forEach { holo -> holo.delete() } }
        holograms.clear()
    }

    fun cancel(player: Player) {
        holograms.remove(player.name)?.forEach { it.delete() }
    }

    fun create(player: Player) {
        if (holograms.containsKey(player.name)) {
            return
        }
        val list = Lists.newArrayList<Hologram>()
        holoContent.forEachIndexed { index, content ->
            val hologram = THologram.create(location.clone().add(0.0, (((holoContent.size - 1) - index) * 0.3), 0.0), content.toFunction(player))
            if (check(player) && content.isNotEmpty()) {
                hologram.addViewer(player)
            }
            list.add(hologram)
        }
        holograms[player.name] = list
    }

    fun refresh(player: Player) {
        if (holograms.containsKey(player.name)) {
            val list = holograms[player.name]!!
            holoContent.forEachIndexed { index, content ->
                val hologram = list.getOrNull(index) ?: return@forEachIndexed
                if (check(player) && content.isNotEmpty()) {
                    hologram.addViewer(player)
                    val text = content.toFunction(player)
                    if (hologram.text != text) {
                        hologram.flash(text)
                    }
                } else {
                    hologram.removeViewer(player)
                }
            }
        } else {
            create(player)
        }
    }

    fun String.toFunction(player: Player): String {
        return if (toFunction) FunctionParser.parseAll(NoneProgram(player), TLocale.Translate.setColored(this)) else TLocale.Translate.setColored(this)
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑全息 $id")
                .rows(3)
                .build { inv ->
                    inv.setItem(11, ItemBuilder(Materials.PISTON.parseMaterial()).name("§f移动").lore("§7左键 + 0.1", "§7右键 - 0.1", "", "§8关闭后生效").build())
                    inv.setItem(13, ItemBuilder(Materials.BOOKSHELF.parseMaterial()).name("§f内容").lore(holoContent.map { "§7$it" }).build())
                    inv.setItem(15, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f条件").lore(holoCondition.map { "§7$it" }).build())
                }.event {
                    it.isCancelled = true
                    when (it.rawSlot) {
                        11 -> {
                            if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                location.add(0.0, 0.1, 0.0)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                location.subtract(0.0, 0.1, 0.0)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                        }
                        13 -> {
                            player.closeInventory()
                            CronusUtils.addItem(player, ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(holoContent.joinToString("\n")).build()).name("§f§f§f编辑内容").lore("§7Hologram", "§7$id").build())
                        }
                        15 -> {
                            player.closeInventory()
                            CronusUtils.addItem(player, ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(holoCondition.joinToString("\n")).build()).name("§f§f§f编辑条件").lore("§7Hologram", "§7$id").build())
                        }
                    }
                }.close {
                    ink.ptms.sandalphon.module.impl.holographic.Hologram.export()
                    init()
                }.open(player)
    }
}