package ink.ptms.sandalphon.module.impl.blockmine.data

import com.google.gson.annotations.Expose
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.CommandBlockControl
import ink.ptms.sandalphon.module.impl.blockmine.event.BlockGrowEvent
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.block.data.Directional
import taboolib.common.platform.function.submit
import taboolib.common.util.random
import taboolib.module.nms.MinecraftVersion

/**
 * @author sky
 * @since 2020-06-01 13:35
 */
class BlockData(@Expose val id: String) {

    @Expose
    val progress = ArrayList<BlockProgress>()

    @Expose
    var growTime = 60

    @Expose
    var growChange = 1.0

    @Expose
    val blocks = ArrayList<BlockState>()

    fun find(block: Location): Pair<BlockState, BlockStructure>? {
        blocks.filter { it.location.world?.name == block.world?.name && it.location.distance(block) < 50 }.forEach {
            progress[it.current].structures.forEach { structure ->
                if (it.location.clone().add(structure.offset) == block) {
                    return it to structure
                }
            }
        }
        return null
    }

    fun isBroken(blockState: BlockState): Boolean {
        return progress[blockState.current].structures.all {
            blockState.location.clone().add(it.offset).block.type != it.origin
        }
    }

    fun clean(blockState: BlockState) {
        progress[blockState.current].structures.forEach {
            blockState.location.clone().add(it.offset).block.type = it.replace
        }
    }

    fun build(blockState: BlockState) {
        progress[blockState.current].structures.forEach {
            val block = blockState.location.clone().add(it.offset).block.run {
                if (type == it.origin) {
                    return@forEach
                }
                type = it.origin
                this
            }
            if (isAfter11300) {
                val blockData = block.blockData
                if (blockData is Directional) {
                    block.blockData = blockData.run {
                        facing = it.direction
                        this
                    }
                }
            } else {
                NMS.instance.setBlockData(block, CommandBlockControl.fromBlockFace(it.direction).toByte())
            }
            submit(async = true) {
                block.world.spawnParticle(Particle.EXPLOSION_NORMAL, block.location.add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0.0)
            }
        }
    }

    fun grow(force: Boolean = false) {
        for (blockState in blocks) {
            if (grow(blockState, force)) {
                return
            }
        }
    }

    fun grow(blockState: BlockState, force: Boolean = false): Boolean {
        if (!BlockGrowEvent(this, blockState).call()) {
            return false
        }
        if (!force && (!blockState.update && blockState.current + 1 == progress.size)) {
            blockState.latest = System.currentTimeMillis()
            return false
        }
        if (!force && System.currentTimeMillis() - blockState.latest < (growTime * 1000L)) {
            return false
        }
        if (!force && random(growChange)) {
            if (isBroken(blockState)) {
                blockState.current = 0
            } else if (!blockState.update) {
                clean(blockState)
                blockState.current = if (blockState.current + 1 == progress.size) 0 else blockState.current + 1
            }
            build(blockState)
            blockState.update = false
        }
        blockState.latest = System.currentTimeMillis()
        return true
    }

    companion object {

        val isAfter11300 by lazy { MinecraftVersion.majorLegacy >= 11300 }
    }
}