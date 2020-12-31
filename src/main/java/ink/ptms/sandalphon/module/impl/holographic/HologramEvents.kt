package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.Helper
import io.izzel.taboolib.Version
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.util.chat.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta

/**
 * @Author sky
 * @Since 2020-05-21 13:33
 */
@TListener(depend = ["Cronus"])
class HologramEvents : Listener, Helper {

    @EventHandler
    fun e(e: PlayerJoinEvent) {
        Hologram.holograms.forEach { it.create(e.player) }
    }

    @EventHandler
    fun e(e: PlayerQuitEvent) {
        Hologram.holograms.forEach { it.cancel(e.player) }
    }

    @EventHandler
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑内容") && e.previousBookMeta.lore!![0].unColored() == "Hologram") {
            val hologramData = Hologram.getHologram(e.previousBookMeta.lore!![1].unColored())
            if (hologramData == null) {
                e.player.error("该全息已失效. (${e.previousBookMeta.lore!![1].unColored()})")
            } else {
                hologramData.holoContent.clear()
                val lines = e.newBookMeta.pages.flatMap {
                    TextComponent(it).toPlainText().replace("§0", "").split("\n")
                }
                if (lines[0].unColored() != "clear") {
                    hologramData.holoContent.addAll(lines)
                }
                hologramData.init()
                // sb 1.12
                if (Version.isBefore(Version.v1_13) && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        } else if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].unColored() == "Hologram") {
            val hologramData = Hologram.getHologram(e.previousBookMeta.lore!![1].unColored())
            if (hologramData == null) {
                e.player.error("该全息已失效. (${e.previousBookMeta.lore!![1].unColored()})")
            } else {
                hologramData.holoCondition.clear()
                val lines = e.newBookMeta.pages.flatMap {
                    TextComponent(it).toPlainText().replace("§0", "").split("\n")
                }
                if (lines[0].unColored() != "clear") {
                    hologramData.holoContent.addAll(lines)
                }
                hologramData.init()
                // sb 1.12
                if (Version.isBefore(Version.v1_13) && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        }
    }
}