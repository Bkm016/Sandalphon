package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.module.configuration.createLocal

/**
 * @author sky
 * @since 2020-05-27 11:17
 */
object Hologram {

    val data by lazy { createLocal("module/hologram.yml") }

    val holograms = ArrayList<HologramData>()

    @Awake(LifeCycle.ACTIVE)
    fun import() {
        holograms.clear()
        data.getKeys(false).forEach {
            holograms.add(HologramData(it,
                Utils.toLocation(data.getString("$it.location")!!),
                data.getStringList("$it.content").toMutableList(),
                data.getStringList("$it.condition").toMutableList()))
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        holograms.forEach { holo ->
            data.set("${holo.id}.location", Utils.fromLocation(holo.location))
            data.set("${holo.id}.content", holo.content)
            data.set("${holo.id}.condition", holo.condition)
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun cancel() {
        holograms.forEach { it.cancel() }
    }

    @Schedule(period = 20, async = true)
    fun refresh() {
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