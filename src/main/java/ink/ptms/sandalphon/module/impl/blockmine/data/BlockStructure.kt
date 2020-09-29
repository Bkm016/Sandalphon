package ink.ptms.sandalphon.module.impl.blockmine.data

import io.izzel.taboolib.internal.gson.annotations.Expose
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector

/**
 * @Author sky
 * @Since 2020-06-01 16:11
 */
class BlockStructure(
        @Expose
        var direction: BlockFace,
        @Expose
        var origin: Material,
        @Expose
        var replace: Material,
        @Expose
        var offset: Vector,
        @Expose
        var tool: String? = null,
        @Expose
        val drop: MutableList<BlockDrop> = ArrayList()
) {

        @Expose
        val metadata = HashMap<String, String>()
}