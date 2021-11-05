package ink.ptms.sandalphon.module.impl.treasurechest.data

import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.treasurechest.event.ChestGenerateEvent
import ink.ptms.sandalphon.util.Utils
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.util.Time
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.type.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.Inventory
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.util.random
import taboolib.common5.Coerce
import taboolib.expansion.getDataContainer
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.toProxyLocation
import java.util.concurrent.CompletableFuture

/**
 * @author sky
 * @since 2020-05-29 21:39
 */
class ChestData(val block: Location) {

    var link: Location? = null
    val open = ArrayList<String>()
    val items = ArrayList<Pair<String, Int>>()
    var title = "箱子"
    var random = -1 to -1
    var update = -1L
    var locked = "null"
    var global = false
    var globalTime = 0L
    var globalInventory: Inventory? = null

    var replace = block.block.type
    val condition = ArrayList<String>()

    val isHighVersion by lazy { MinecraftVersion.majorLegacy >= 11300 }

    init {
        // 判断大箱子
        kotlin.runCatching {
            val inventory = (block.block.state as Container).inventory
            if (inventory is DoubleChestInventory) {
                link = if (inventory.leftSide.location == block) {
                    inventory.rightSide.location!!
                } else {
                    inventory.leftSide.location!!
                }
            }
        }
    }

    fun isBlock(block: Block): Boolean {
        return this.block == block.location || block.location == link
    }

    fun check(player: Player): CompletableFuture<Boolean> {
        return if (condition.isEmpty()) {
            CompletableFuture.completedFuture(true)
        } else {
            try {
                KetherShell.eval(condition, sender = adaptCommandSender(player)).thenApply {
                    Coerce.toBoolean(it)
                }
            } catch (e: Throwable) {
                e.printKetherErrorMessage()
                CompletableFuture.completedFuture(false)
            }
        }
    }

    fun update(player: Player, inventory: Inventory): Inventory {
        val content = (0 until inventory.size).toMutableList()
        val given = ArrayList<Pair<String, Int>>()
        if (random == -1 to -1) {
            given.addAll(items)
        } else if (items.isNotEmpty()) {
            val random = random(random.first, random.second.coerceAtMost(items.size).coerceAtLeast(random.first))
            items.toList().toMutableList().run {
                (1..random).forEach { _ -> given.add(removeAt(random(size))) }
            }
        }
        given.forEach { (k, v) ->
            val item = ZaphkielAPI.getItem(k, player) ?: return@forEach
            val event = ChestGenerateEvent(player, this, item, v)
            event.call()
            if (!event.isCancelled) {
                val itemStack = event.item.rebuildToItemStack(player)
                itemStack.amount = event.amount
                inventory.setItem(content.removeAt(random(content.size)), itemStack)
            }
        }
        return inventory
    }

    fun tick() {
        if (open.isNotEmpty()) {
            block.world!!.players.forEach { NMS.INSTANCE.sendBlockAction(it, block.block, 1, 1) }
        }
        block.world!!.players.forEach { tick(it) }
    }

    fun tick(player: Player, particle: Boolean = false) {
        if (player.world.name != block.world?.name || player.location.distance(block) > 50) {
            return
        }
        if (block.block.type == replace) {
            return
        }
        if (global) {
            if (globalTime > System.currentTimeMillis()) {
                if (particle) {
                    player.playSound(block, Sound.BLOCK_WOOD_BREAK, 1f, random(0.8, 1.2).toFloat())
                    player.spawnParticle(Particle.CRIT, block.clone().add(0.5, 0.5, 0.5), 50, 0.0, 0.0, 0.0, 0.5)
                }
                if (isHighVersion) {
                    player.sendBlockChange(block, replace.createBlockData())
                } else {
                    player.sendBlockChange(block, replace, 0)
                }
            } else {
                if (isHighVersion) {
                    player.sendBlockChange(block, block.block.blockData)
                } else {
                    player.sendBlockChange(block, block.block.type, block.block.data)
                }
            }
        } else {
            val data = player.getDataContainer()
            val time = Coerce.toLong(data["treasurechest.${Utils.fromLocation(block).replace(".", "__")}"])
            if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                if (particle) {
                    player.playSound(block, Sound.BLOCK_WOOD_BREAK, 1f, random(0.8, 1.2).toFloat())
                    // 播放粒子效果
                    ProxyParticle.CRIT.sendTo(
                        player = adaptPlayer(player),
                        location = block.clone().add(0.5, 0.5, 0.5).toProxyLocation(),
                        speed = 0.5,
                        count = 50
                    )
                }
                if (isHighVersion) {
                    player.sendBlockChange(block, replace.createBlockData())
                } else {
                    player.sendBlockChange(block, replace, 0)
                }
            } else {
                if (isHighVersion) {
                    player.sendBlockChange(block, block.block.blockData)
                } else {
                    player.sendBlockChange(block, block.block.type, block.block.data)
                }
            }
        }
    }

    fun getTimeDisplay(from: Long): String {
        val time = Time(from)
        return "${time.days.isSet("%天")}${time.hours.isSet("%时")}${time.minutes.isSet("%分")}${time.seconds.isSet("%秒")}".ifEmpty { "无" }
    }

    fun getTimeFormatted(from: Long): String {
        val time = Time(from)
        return "${time.days.isSet("%d")}${time.hours.isSet("%h")}${time.minutes.isSet("%m")}${time.seconds.isSet("%s")}".ifEmpty { "-1" }
    }

    private fun Long.isSet(value: String): String {
        return if (this > 0) value.replace("%", toString()) else ""
    }
}