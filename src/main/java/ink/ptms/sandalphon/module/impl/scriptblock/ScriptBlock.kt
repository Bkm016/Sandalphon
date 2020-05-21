package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockData
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusParser
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

object ScriptBlock {

    @LocalFile("module/scriptblock.yml")
    lateinit var data: FileConfiguration
        private set

    val blocks = ArrayList<BlockData>()

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
            return
        }
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
        data.loadFromString("")
        blocks.forEach { block ->
            val location = Utils.fromLocation(block.block).replace(".", "__")
            data.set("$location.link", block.link.map { Utils.fromLocation(it) })
            data.set("$location.type", block.blockType.name)
            data.set("$location.action", block.blockAction)
            data.set("$location.condition", block.blockCondition)
        }
    }

    fun getBlock(block: Block): BlockData? {
        return blocks.firstOrNull { it.isBlock(block) }
    }
}