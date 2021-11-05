package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.Helper
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.chat.uncolored
import taboolib.module.nms.MinecraftVersion

/**
 * @author sky
 * @since 2020-05-21 13:33
 */
object HologramEvents : Helper {

    @SubscribeEvent
    fun e(e: PlayerJoinEvent) {
        Hologram.holograms.forEach { it.create(e.player) }
    }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        Hologram.holograms.forEach { it.cancel(e.player) }
    }

    @SubscribeEvent
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑内容") && e.previousBookMeta.lore!![0].uncolored() == "Hologram") {
            val hologramData = Hologram.getHologram(e.previousBookMeta.lore!![1].uncolored())
            if (hologramData == null) {
                e.player.error("该全息已失效. (${e.previousBookMeta.lore!![1].uncolored()})")
            } else {
                hologramData.content.clear()
                val lines = e.newBookMeta.pages.flatMap {
                    TextComponent(it).toPlainText().replace("§0", "").split("\n")
                }
                if (lines[0].uncolored() != "clear") {
                    hologramData.content.addAll(lines)
                }
                hologramData.init()
                // sb 1.12
                if (MinecraftVersion.majorLegacy < 11300 && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        } else if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].uncolored() == "Hologram") {
            val hologramData = Hologram.getHologram(e.previousBookMeta.lore!![1].uncolored())
            if (hologramData == null) {
                e.player.error("该全息已失效. (${e.previousBookMeta.lore!![1].uncolored()})")
            } else {
                hologramData.condition.clear()
                val lines = e.newBookMeta.pages.flatMap {
                    TextComponent(it).toPlainText().replace("§0", "").split("\n")
                }
                if (lines[0].uncolored() != "clear") {
                    hologramData.content.addAll(lines)
                }
                hologramData.init()
                // sb 1.12
                if (MinecraftVersion.majorLegacy < 11300 && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        }
    }
}