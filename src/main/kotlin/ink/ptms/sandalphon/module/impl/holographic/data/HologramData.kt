package ink.ptms.sandalphon.module.impl.holographic.data

import ink.ptms.adyeshach.api.AdyeshachAPI
import ink.ptms.adyeshach.api.Hologram
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.Coerce
import taboolib.module.kether.KetherFunction
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class HologramData(val id: String, var location: Location, val content: MutableList<String> = ArrayList(), var condition: MutableList<String> = ArrayList()) {

    val holograms: MutableMap<String, Hologram<*>> = ConcurrentHashMap()

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
                KetherShell.eval(condition, sender = adaptPlayer(player)).thenApply { Coerce.toBoolean(it) }
            } catch (e: Throwable) {
                e.printKetherErrorMessage()
                CompletableFuture.completedFuture(false)
            }
        }
    }

    fun cancel() {
        holograms.forEach { it.value.delete() }
        holograms.clear()
    }

    fun cancel(player: Player) {
        holograms.remove(player.name)?.delete()
    }

    fun create(player: Player) {
        if (player.name !in holograms) {
            check(player).thenAccept {
                if (it) {
                    holograms[player.name] = AdyeshachAPI.createHologram(player, location, content)
                }
            }
        }
    }

    fun refresh(player: Player) {
        if (holograms.containsKey(player.name)) {
            check(player).thenAccept {
                if (it) {
                    holograms[player.name]!!.update(content.map { c -> c.toFunction(player) })
                } else {
                    holograms.remove(player.name)?.delete()
                }
            }
        } else {
            create(player)
        }
    }

    fun String.toFunction(player: Player): String {
        return KetherFunction.parse(this, sender = adaptPlayer(player))
    }
}