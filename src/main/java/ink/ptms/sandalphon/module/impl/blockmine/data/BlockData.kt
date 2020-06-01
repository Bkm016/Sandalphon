package ink.ptms.sandalphon.module.impl.blockmine.data

import ink.ptms.sandalphon.module.impl.blockmine.BlockMine
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.internal.gson.annotations.Expose
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @Author sky
 * @Since 2020-06-01 13:35
 */
class BlockData(@Expose val id: String) {

    @Expose
    val progress = ArrayList<BlockProgress>()

    @Expose
    var growTime = 60

    @Expose
    var growChange = 1.0

    @Expose
    val blocks = HashMap<Location, BlockState>()

    fun find(block: Location): Location? {
        return blocks.entries.firstOrNull { it.key.world?.name == block.world?.name && it.key.distance(block) < 50 && progress[it.value.current].structures.any { block.clone().add(it.offset) == block } }?.key
    }

    fun isBroken(origin: Location): Boolean {
        val triple = blocks[origin]!!
        return progress[triple.current].structures.all { origin.clone().add(it.offset).block.type != it.origin }
    }

    fun clean(origin: Location) {
        val triple = blocks[origin]!!
        progress[triple.current].structures.forEach { origin.clone().add(it.offset).block.type = it.replace }
    }

    fun build(origin: Location) {
        val triple = blocks[origin]!!
        progress[triple.current].structures.forEach {
            val block = origin.clone().add(it.offset).block.run {
                this.type = it.origin
                this
            }
            val blockData = block.blockData
            if (blockData is Directional) {
                block.blockData = blockData.run {
                    this.facing = it.direction
                    this
                }
            }
        }
    }

    fun grow(force: Boolean = false) {
        for (pair in blocks) {
            if (grow(pair.key, force)) {
                return
            }
        }
    }

    fun grow(origin: Location, force: Boolean = false): Boolean {
        val triple = blocks[origin]!!
        if (!triple.update && triple.current + 1 == progress.size) {
            return false
        }
        if (!force && System.currentTimeMillis() - triple.latest < (growTime * 1000L)) {
            return false
        }
        if (Numbers.random(growChange)) {
            if (isBroken(origin)) {
                triple.current = 0
            } else if (!triple.update) {
                clean(origin)
                triple.current = if (triple.current + 1 == progress.size) 0 else triple.current + 1
            }
            build(origin)
        }
        triple.latest = System.currentTimeMillis()
        return true
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑开采结构 $id")
                .rows(3)
                .build { inv ->
                    inv.setItem(12, ItemBuilder(Material.STRUCTURE_VOID).name("§f阶段 (${progress.size})").lore(progress.mapIndexed { index, progress ->
                        "§7第 ${index + 1} 阶段包含 ${progress.structures} 个结构"
                    }.toMutableList().run {
                        this.add("")
                        this.add("§8点击编辑")
                        this
                    }).build())
                    inv.setItem(14, ItemBuilder(Material.BOOKSHELF).name("§f生长").lore("§7时间: ${growTime}秒 §8(左键编辑)", "§7几率: ${growChange * 100}% §8(右键编辑)").build())
                }.event {
                    it.isCancelled = true
                    when (it.rawSlot) {
                        12 -> {

                        }
                        14 -> {

                        }
                    }
                }.close {
                    BlockMine.export()
                }.open(player)
    }
}