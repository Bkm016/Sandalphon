package ink.ptms.sandalphon.module.impl.holographic.data

import com.google.common.collect.Lists
import ink.ptms.sandalphon.util.Utils.print
import io.izzel.taboolib.kotlin.kether.KetherFunction
import io.izzel.taboolib.kotlin.kether.KetherShell
import io.izzel.taboolib.kotlin.kether.common.util.LocalizedException
import io.izzel.taboolib.module.hologram.Hologram
import io.izzel.taboolib.module.hologram.THologram
import io.izzel.taboolib.module.locale.TLocale
import io.izzel.taboolib.util.Coerce
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Materials
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class HologramData(val id: String, var location: Location, val content: MutableList<String> = ArrayList(), var condition: MutableList<String> = ArrayList()) {

    val holograms = HashMap<String, List<Hologram>>()

    init {
        init()
    }

    fun init() {
        cancel()
        Bukkit.getOnlinePlayers().forEach { create(it) }
    }

    fun check(player: Player): CompletableFuture<Boolean> {
        return if (condition.isEmpty()) {
            CompletableFuture.completedFuture(true)
        } else {
            try {
                KetherShell.eval(condition) {
                    sender = player
                }.thenApply {
                    Coerce.toBoolean(it)
                }
            } catch (e: LocalizedException) {
                e.print()
                CompletableFuture.completedFuture(false)
            } catch (e: Throwable) {
                e.printStackTrace()
                CompletableFuture.completedFuture(false)
            }
        }
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
        val holograms = Lists.newArrayList<Hologram>()
        content.forEachIndexed { index, content ->
            val hologram = THologram.create(location.clone().add(0.0, (((this.content.size - 1) - index) * 0.3), 0.0), TLocale.Translate.setColored(content.toFunction(player)))
            if (content.isNotEmpty()) {
                check(player).thenAccept {
                    if (it) {
                        hologram.addViewer(player)
                    }
                }
            }
            holograms.add(hologram)
        }
        this.holograms[player.name] = holograms
    }

    fun refresh(player: Player) {
        if (holograms.containsKey(player.name)) {
            val list = holograms[player.name]!!
            content.forEachIndexed { index, content ->
                val hologram = list.getOrNull(index) ?: return@forEachIndexed
                if (content.isEmpty()) {
                    hologram.removeViewer(player)
                } else {
                    check(player).thenAccept {
                        if (it) {
                            hologram.addViewer(player)
                            val text = content.toFunction(player)
                            if (hologram.text != text) {
                                hologram.flash(text)
                            }
                        } else {
                            hologram.removeViewer(player)
                        }
                    }
                }
            }
        } else {
            create(player)
        }
    }

    fun String.toFunction(player: Player): String {
        return KetherFunction.parse(this) {
            sender = player
        }
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑全息 $id")
                .rows(3)
                .build { inv ->
                    inv.setItem(11, ItemBuilder(Materials.PISTON.parseMaterial()).name("§f移动").lore("§7左键 + 0.1", "§7右键 - 0.1", "", "§8关闭后生效").build())
                    inv.setItem(13, ItemBuilder(Materials.BOOKSHELF.parseMaterial()).name("§f内容").lore(content.map { "§7$it" }).build())
                    inv.setItem(15, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f条件").lore(condition.map { "§7$it" }).build())
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
                            val item = ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(content.joinToString("\n")).build()).name("§f§f§f编辑内容").lore("§7Hologram", "§7$id").build()
                            player.closeInventory()
                            player.inventory.addItem(item)
                        }
                        15 -> {
                            val item = ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(condition.joinToString("\n")).build()).name("§f§f§f编辑条件").lore("§7Hologram", "§7$id").build()
                            player.closeInventory()
                            player.inventory.addItem(item)
                        }
                    }
                }.close {
                    ink.ptms.sandalphon.module.impl.holographic.Hologram.export()
                    init()
                }.open(player)
    }
}