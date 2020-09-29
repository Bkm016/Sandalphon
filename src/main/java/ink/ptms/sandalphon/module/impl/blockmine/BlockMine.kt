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
import io.izzel.taboolib.module.db.local.SecuredFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.module.packet.Packet
import io.izzel.taboolib.module.packet.TPacket
import io.izzel.taboolib.module.tellraw.TellrawCreator
import io.izzel.taboolib.module.tellraw.TellrawJson
import io.izzel.taboolib.util.Files
import io.izzel.taboolib.util.item.Items
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.nio.charset.StandardCharsets

object BlockMine {

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
        Files.folder(Sandalphon.plugin.dataFolder, "module/blockmine").listFiles()?.map { file ->
            if (file.name.endsWith(".json")) {
                blocks.add(Utils.serializer.fromJson(file.readText(StandardCharsets.UTF_8), BlockData::class.java))
            }
        }
        cached()
        println("[Sandalphon] Loaded ${blocks.size} structure blocks.")
    }

    @TFunction.Cancel
    @TSchedule(period = 20 * 60, async = true)
    fun export() {
        blocks.forEach { block ->
            Files.file(Sandalphon.plugin.dataFolder, "module/blockmine/${block.id}.json").writeText(Utils.format(Utils.serializer.toJsonTree(block)), StandardCharsets.UTF_8)
        }
    }

    fun cached() {
        blocksCache.clear()
        blocks.forEach { b ->
            b.progress.forEach { p ->
                p.structures.forEach {
                    blocksCache.add(it.origin)
                    blocksCache.add(it.replace)
                }
            }
        }
    }

    fun delete(id: String) {
        Files.deepDelete(File(Sandalphon.plugin.dataFolder, "module/blockmine/$id.json"))
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