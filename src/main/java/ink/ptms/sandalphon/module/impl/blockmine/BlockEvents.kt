package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.CommandBlockControl
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockState
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockStructure
import ink.ptms.sandalphon.util.Utils
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.module.nms.nbt.NBTBase
import io.izzel.taboolib.module.nms.nbt.NBTList
import io.izzel.taboolib.util.KV
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.lite.Effects
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

/**
 * @author sky
 * @since 2020-06-01 21:43
 */
@TListener
class BlockEvents : Listener, Helper {

    val catcher = HashMap<String, KV<Location?, Location?>>()

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun player(e: BlockBreakEvent) {
        val pair = BlockMine.find(e.block.location) ?: return
        e.isCancelled = true
        if (pair.second.second.origin == e.block.type) {
            if (pair.second.second.tool != null) {
                if (Utils.asgardHook) {
                    if (!Items.hasLore(e.player.inventory.itemInMainHand, pair.second.second.tool)) {
                        return
                    }
                } else {
                    val itemStream = ZaphkielAPI.read(e.player.inventory.itemInMainHand)
                    if (itemStream.isVanilla() || !itemStream.getZaphkielData().containsKey("blockmine")) {
                        return
                    }
                    val blockmine = itemStream.getZaphkielData()["blockmine"] as NBTList
                    if (!blockmine.contains(NBTBase(pair.second.second.tool))) {
                        return
                    }
                }
            }
            e.block.type = pair.second.second.replace
            e.block.world.players.filter { it != e.player }.forEach {
                it.playEffect(e.block.location, Effect.STEP_SOUND, pair.second.second.origin)
            }
            pair.second.first.update = true
            pair.second.second.drop.filter { Numbers.random(it.chance) }.forEach {
                e.block.world.dropItem(e.block.location.add(0.5, 0.5, 0.5), (Utils.item(it.item, e.player) ?: return@forEach).run {
                    this.amount = it.amount
                    this
                }).pickupDelay = 20
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun e(e: BlockBreakEvent) {
        if (e.player.isOp) {
            if (Items.hasName(e.player.inventory.itemInMainHand, "场景魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val blockData = BlockMine.getBlock(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored())
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                if (blockData.find(e.block.location) != null) {
                    e.player.error("该位置已存在实例.")
                    return
                }
                val blockState = BlockState(e.block.location, blockData.progress.size - 1, 0, false)
                blockData.blocks.add(blockState)
                blockData.build(blockState)
                e.player.info("实例已创建.")
                BlockMine.export()
            }
            if (Items.hasName(e.player.inventory.itemInMainHand, "调试魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val blockData = BlockMine.getBlock(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored())
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val pair = blockData.find(e.block.location)
                if (pair == null) {
                    e.player.error("该位置不存在实例.")
                    return
                }
                blockData.build(pair.first)
                e.player.info("实例已重建.")
            }
            if (Items.hasName(e.player.inventory.itemInMainHand, "捕获魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val args = e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored().split(" ")
                val blockData = BlockMine.getBlock(args[0])
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val blockProgress = blockData.progress.getOrNull(args[1].toInt())
                if (blockProgress == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val pair = catcher.computeIfAbsent(e.player.name) { KV(null, null) }
                pair.key = e.block.location
                if (pair.key != null && pair.value != null) {
                    build(e.player, pair.key!!, pair.value!!) { loc ->
                        Effects.create(Particle.FLAME, loc.add(0.5, 0.5, 0.5)).count(5).player(e.player).play()
                    }
                }
                e.player.info("起点已创建.")
                BlockMine.export()
            }
        }
    }

    @EventHandler
    fun e(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        if (e.player.isOp && e.action == Action.RIGHT_CLICK_BLOCK) {
            if (Items.hasName(e.player.inventory.itemInMainHand, "场景魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val blockData = BlockMine.getBlock(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored())
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val mid = blockData.find(e.clickedBlock!!.location)
                if (mid == null) {
                    e.player.error("该位置不存在实例.")
                    return
                }
                blockData.clean(mid.first)
                blockData.blocks.remove(mid.first)
                e.player.info("实例已删除.")
                BlockMine.export()
            }
            if (Items.hasName(e.player.inventory.itemInMainHand, "调试魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val blockData = BlockMine.getBlock(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored())
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val pair = blockData.find(e.clickedBlock!!.location)
                if (pair == null) {
                    e.player.error("该位置不存在实例.")
                    return
                }
                blockData.clean(pair.first)
                pair.first.current = if (pair.first.current + 1 == blockData.progress.size) 0 else pair.first.current + 1
                blockData.build(pair.first)
                e.player.info("实例阶段已切换.")
            }
            if (Items.hasName(e.player.inventory.itemInMainHand, "捕获魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "BlockMine")) {
                e.isCancelled = true
                val args = e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].unColored().split(" ")
                val blockData = BlockMine.getBlock(args[0])
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val blockProgress = blockData.progress.getOrNull(args[1].toInt())
                if (blockProgress == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val pair = catcher.computeIfAbsent(e.player.name) { KV(null, null) }
                pair.value = e.clickedBlock!!.location
                if (pair.key != null && pair.value != null) {
                    build(e.player, pair.key!!, pair.value!!) { loc ->
                        Effects.create(Particle.FLAME, loc.add(0.5, 0.5, 0.5)).count(5).player(e.player).play()
                    }
                }
                e.player.info("终点已创建.")
                BlockMine.export()
            }
        }
    }

    @EventHandler
    fun e(e: PlayerDropItemEvent) {
        if (e.player.isOp) {
            val item = e.itemDrop.itemStack
            if (Items.hasName(item, "捕获魔杖") && Items.hasLore(item, "BlockMine")) {
                e.isCancelled = true
                val args = item.itemMeta!!.lore!![1].unColored().split(" ")
                val blockData = BlockMine.getBlock(args[0])
                if (blockData == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val blockProgress = blockData.progress.getOrNull(args[1].toInt())
                if (blockProgress == null) {
                    e.player.error("该魔杖已失效.")
                    return
                }
                val pair = catcher.remove(e.player.name) ?: KV(null, null)
                if (pair.key == null || pair.value == null) {
                    e.player.error("起点或终点缺失.")
                    return
                }
                val mid = pair.key!!.toVector().midpoint(pair.value!!.toVector()).toLocation(e.player.world).run {
                    this.y = min(pair.key!!.y, pair.value!!.y)
                    this.block.location
                }
                blockProgress.structures.clear()
                build(e.player, pair.key!!, pair.value!!, true) { loc ->
                    Effects.create(Particle.VILLAGER_HAPPY, loc.add(0.5, 0.5, 0.5)).count(5).player(e.player).play()
                    val block = loc.block
                    if (block.type == Material.AIR) {
                        return@build
                    }
                    val direction = CommandBlockControl.getBlockFace(block)
                    val structure = BlockStructure(direction, block.type, Material.AIR, block.location.subtract(mid).toVector())
                    blockProgress.structures.add(structure)
                }
                e.player.info("结构已储存.")
                BlockMine.export()
                BlockMine.cached()
            }
        }
    }

    fun build(player: Player, locA: Location, locB: Location, filled: Boolean = false, action: (Location) -> (Unit)) {
        val maxX = max(locA.x, locB.x)
        val minX = min(locA.x, locB.x)
        val maxY = max(locA.y, locB.y)
        val minY = min(locA.y, locB.y)
        val maxZ = max(locA.z, locB.z)
        val minZ = min(locA.z, locB.z)
        buildCube(Location(player.world, minX, minY, minZ), Location(player.world, maxX, maxY, maxZ), 1.0, filled) { loc ->
            action.invoke(loc)
        }
    }

    fun buildCube(start: Location, end: Location, rate: Double, filled: Boolean = false, action: (Location) -> (Unit)) {
        val maxX = max(start.x, end.x)
        val minX = min(start.x, end.x)
        val maxY = max(start.y, end.y)
        val minY = min(start.y, end.y)
        val maxZ = max(start.z, end.z)
        val minZ = min(start.z, end.z)
        var x = minX
        while (x <= maxX) {
            var y = minY
            while (y <= maxY) {
                var z = minZ
                while (z <= maxZ) {
                    if (filled || (y == minY || y + rate > maxY || x == minX || x + rate > maxX || z == minZ || z + rate > maxZ)) {
                        action.invoke(start.clone().add(x - minX, y - minY, z - minZ))
                    }
                    z += rate
                }
                y += rate
            }
            x += rate
        }
    }
}