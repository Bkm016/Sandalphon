package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockData
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.util.Utils
import org.bukkit.block.Block
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.createLocal

object ScriptBlock {

    val data by lazy { createLocal("module/scriptblock.yml") }
    val blocks = ArrayList<BlockData>()

    @Awake(LifeCycle.ACTIVE)
    fun import() {
        blocks.clear()
        data.getKeys(false).forEach {
            blocks.add(BlockData(Utils.toLocation(it.replace("__", ".")),
                BlockType.valueOf(data.getString("$it.type")!!),
                data.getStringList("$it.action").toMutableList(),
                data.getStringList("$it.condition").toMutableList()).run {
                this.link.addAll(data.getStringList("$it.link").map { link -> Utils.toLocation(link) })
                this
            })
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun export() {
        data.getKeys(false).forEach { data[it] = null }
        blocks.forEach { block ->
            val location = Utils.fromLocation(block.block).replace(".", "__")
            data["$location.link"] = block.link.map { Utils.fromLocation(it) }
            data["$location.type"] = block.blockType.name
            data["$location.action"] = block.action
            data["$location.condition"] = block.condition
        }
    }

    fun delete(location: String) {
        data[location.replace(".", "__")] = null
    }

    fun getBlock(block: Block): BlockData? {
        return blocks.firstOrNull { it.isBlock(block) }
    }
}