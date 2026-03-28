package ink.ptms.sandalphon.module.impl.gather.data

import org.bukkit.Material
import org.bukkit.block.BlockFace

/**
 * 单个方块结构定义
 */
data class GatherBlock(
    var material: Material = Material.STONE,
    var replace: Material = Material.AIR,
    var offset: List<Double> = listOf(0.0, 0.0, 0.0),
    var direction: BlockFace = BlockFace.NORTH
) {
    val offsetX get() = offset.getOrElse(0) { 0.0 }
    val offsetY get() = offset.getOrElse(1) { 0.0 }
    val offsetZ get() = offset.getOrElse(2) { 0.0 }
}
