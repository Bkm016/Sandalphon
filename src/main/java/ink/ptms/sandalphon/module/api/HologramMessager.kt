package ink.ptms.sandalphon.module.api

import ink.ptms.sandalphon.util.Utils.printed
import io.izzel.taboolib.kotlin.Tasks
import io.izzel.taboolib.module.hologram.Hologram
import io.izzel.taboolib.module.hologram.THologram
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.module.locale.TLocale
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

@TListener(cancel = "cancel")
object HologramMessager : Listener {

    private val messageMap = ConcurrentHashMap<String, MutableMap<String, Message>>()

    fun send(player: Player, location: Location, vararg message: String) {
        val key = "${location.world!!.name},${location.x},${location.y},${location.z}"
        val messages = messageMap.computeIfAbsent(player.name) { ConcurrentHashMap() }
        if (messages.containsKey(key)) {
            return
        }
        val obj = Message(player, location, TLocale.Translate.setColored(message.toList()))
        messages[key] = obj
        Tasks.delay(40) {
            messages.remove(key)
            obj.cancel()
        }
    }

    fun cancel() {
        messageMap.forEach { it.value.forEach { message -> message.value.cancel() } }
    }

    @EventHandler
    fun e(e: PlayerQuitEvent) {
        messageMap.remove(e.player.name)?.forEach { it.value.cancel() }
    }

    class Message(val player: Player, val location: Location, val message: List<String>) {

        val holograms = ArrayList<Hologram>()
        val time = System.currentTimeMillis()

        init {
            message.forEachIndexed { index, content ->
                holograms.add(THologram.create(location.clone().add(0.0, (((message.size - 1) - index) * 0.3), 0.0), content).also {
                    if (content.isNotEmpty()) {
                        it.addViewer(player)
                        it.flash(content.printed("_"), 1)
                    }
                })
            }
        }

        fun cancel() {
            holograms.forEach { it.delete() }
        }
    }
}