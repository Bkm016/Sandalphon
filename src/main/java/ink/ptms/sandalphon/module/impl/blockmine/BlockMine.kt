package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockProgress
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockStructure
import ink.ptms.sandalphon.module.impl.holographic.Hologram
import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import ink.ptms.sandalphon.module.impl.treasurechest.TreasureChest
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.internal.gson.GsonBuilder
import io.izzel.taboolib.internal.gson.JsonDeserializer
import io.izzel.taboolib.internal.gson.JsonPrimitive
import io.izzel.taboolib.internal.gson.JsonSerializer
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.module.packet.Packet
import io.izzel.taboolib.module.packet.TPacket
import io.izzel.taboolib.module.tellraw.TellrawCreator
import io.izzel.taboolib.module.tellraw.TellrawJson
import io.izzel.taboolib.util.item.Items
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

object BlockMine {

    @LocalFile("module/blockmine.yml")
    lateinit var data: FileConfiguration
        private set

    val blocks = ArrayList<BlockData>()
    val blocksCache = HashSet<Material>()

    @TSchedule(period = 20)
    fun e() {
        blocks.forEach { it.grow() }
    }

    @Suppress("UNCHECKED_CAST")
    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            return
        }
        blocks.clear()
        data.getKeys(false).forEach {
            blocks.add(Utils.serializer.fromJson(data.getString(it)!!, BlockData::class.java))
        }
        cached()
    }

    @TFunction.Cancel
    @TSchedule(period = 20 * 60, async = true)
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        blocks.forEach { block ->
            data.set(block.id, Utils.format(Utils.serializer.toJsonTree(block)))
        }
    }

    fun cached() {
        blocksCache.clear()
        blocksCache.addAll(blocks.flatMap { b -> b.progress.flatMap { p -> p.structures.flatMap { listOf(it.origin, it.replace) } } })
    }

    fun delete(id: String) {
        data.set(id, null)
    }

    fun find(block: Location): Pair<BlockData, Pair<BlockState, BlockStructure>>? {
        if (block.block.type !in blocksCache) {
            return null
        }
        for (b in blocks) {
            val find = b.find(block)
            if (find != null) {
                return b to find
            }
        }
        return null
    }

    fun getBlock(id: String): BlockData? {
        return blocks.firstOrNull { it.id == id }
    }
}