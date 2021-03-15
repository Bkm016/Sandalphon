package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockData
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration

object ScriptBlock {

    @LocalFile("module/scriptblock.yml")
    lateinit var data: FileConfiguration
        private set

    val blocks = ArrayList<BlockData>()

    @TSchedule
    fun import() {
        blocks.clear()
        data.getKeys(false).forEach {
            blocks.add(BlockData(Utils.toLocation(it.replace("__", ".")), BlockType.valueOf(data.getString("$it.type")!!), data.getStringList("$it.action"), data.getStringList("$it.condition")).run {
                this.link.addAll(data.getStringList("$it.link").map { link -> Utils.toLocation(link) })
                this
            })
        }
    }

    @TFunction.Cancel
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        blocks.forEach { block ->
            val location = Utils.fromLocation(block.block).replace(".", "__")
            data.set("$location.link", block.link.map { Utils.fromLocation(it) })
            data.set("$location.type", block.blockType.name)
            data.set("$location.action", block.action)
            data.set("$location.condition", block.condition)
        }
    }

    fun delete(location: String) {
        data.set(location.replace(".", "__"), null)
    }

    fun getBlock(block: Block): BlockData? {
        return blocks.firstOrNull { it.isBlock(block) }
    }
}