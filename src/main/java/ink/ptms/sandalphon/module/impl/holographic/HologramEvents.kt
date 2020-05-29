package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.Helper
import io.izzel.taboolib.module.inject.TListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

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
                if (e.newBookMeta.pages[0].unColored() != "clear") {
                    hologramData.holoContent.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                hologramData.init()
            }
        } else if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].unColored() == "Hologram") {
            val hologramData = Hologram.getHologram(e.previousBookMeta.lore!![1].unColored())
            if (hologramData == null) {
                e.player.error("该全息已失效. (${e.previousBookMeta.lore!![1].unColored()})")
            } else {
                hologramData.holoCondition.clear()
                if (e.newBookMeta.pages[0].unColored() != "clear") {
                    hologramData.holoCondition.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                hologramData.init()
            }
        }
    }
}