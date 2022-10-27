package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockStructure
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import taboolib.common.LifeCycle
import taboolib.common.io.newFile
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.nio.charset.StandardCharsets

object BlockMine {

    val blocks = ArrayList<BlockData>()
    val blocksCache = HashSet<Material>()

    @Schedule(period = 20, async = true)
    fun e() {
        blocks.forEach { it.grow() }
    }

    @Awake(LifeCycle.ACTIVE)
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
            return
        }
        // 清空缓存文件
        blocks.clear()
        // 加载缓存文件
        newFile(getDataFolder(), "module/blockmine", create = false, folder = true).listFiles()?.map { file ->
            if (file.name.endsWith(".json")) {
                blocks.add(Utils.serializer.fromJson(file.readText(StandardCharsets.UTF_8), BlockData::class.java))
            }
        }
        // 加载材质缓存
        loadBlockCache()
    }

    @Awake(LifeCycle.DISABLE)
    @Schedule(period = 20 * 60, async = true)
    fun export() {
        blocks.forEach { block ->
            // 写入文件
            newFile(getDataFolder(), "module/blockmine/${block.id}.json").writeText(Utils.format(Utils.serializer.toJsonTree(block)), StandardCharsets.UTF_8)
        }
    }

    fun loadBlockCache() {
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
        File(getDataFolder(), "module/blockmine/$id.json").delete()
    }

    fun find(block: Location): FindResult? {
        if (block.block.type !in blocksCache) {
            return null
        }
        blocks.forEach { b ->
            val find = b.find(block)
            if (find != null) {
                return FindResult(b, find.first, find.second)
            }
        }
        return null
    }

    fun getBlock(id: String): BlockData? {
        return blocks.firstOrNull { it.id == id }
    }

    class FindResult(val blockData: BlockData, val blockState: BlockState, val blockStructure: BlockStructure)
}