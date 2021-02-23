package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration

/**
 * @author sky
 * @since 2020-05-27 11:17
 */
object Hologram {

    @LocalFile("module/hologram.yml")
    lateinit var data: FileConfiguration
        private set

    val holograms = ArrayList<HologramData>()

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
            return
        }
        holograms.clear()
        data.getKeys(false).forEach {
            holograms.add(HologramData(it, Utils.toLocation(data.getString("$it.location")!!), data.getStringList("$it.content"), data.getStringList("$it.condition")))
        }
    }

    @TFunction.Cancel
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        holograms.forEach { holo ->
            data.set("${holo.id}.location", Utils.fromLocation(holo.location))
            data.set("${holo.id}.content", holo.content)
            data.set("${holo.id}.condition", holo.condition)
        }
    }

    @TFunction.Cancel
    fun cancel() {
        holograms.forEach { it.cancel() }
    }

    @TSchedule(period = 20)
    fun e() {
        Bukkit.getOnlinePlayers().forEach { player ->
            holograms.filter { it.location.world?.name == player.world.name }.forEach {
                it.refresh(player)
            }
        }
    }

    fun delete(id: String) {
        data.set(id, null)
    }

    fun getHologram(id: String): HologramData? {
        return holograms.firstOrNull { it.id == id }
    }
}