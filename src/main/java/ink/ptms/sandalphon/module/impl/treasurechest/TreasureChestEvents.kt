package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestInventory
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.module.lite.SimpleReflection
import io.izzel.taboolib.module.nms.NMS
import io.izzel.taboolib.module.packet.Packet
import io.izzel.taboolib.module.packet.TPacket
import io.izzel.taboolib.module.packet.TPacketHandler
import io.izzel.taboolib.module.packet.TPacketListener
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * @Author sky
 * @Since 2020-05-29 22:00
 */
@TListener
class TreasureChestEvents : Listener, Helper {

    init {
        TPacketHandler.addListener(Sandalphon.getPlugin(), object : TPacketListener() {

            override fun onReceive(player: Player, packet: Packet): Boolean {
                if (packet.`is`("PacketPlayInUseItem")) {
                    val a = packet.read("a")
                    if (a.javaClass.simpleName == "BlockPosition") {
                        val pos = NMS.handle().fromBlockPosition(a)
                        val loc = Location(player.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                        val chest = TreasureChest.getChest(loc.block) ?: return true
                        if (packet.read("c").toString() == "MAIN_HAND") {
                            Bukkit.getScheduler().runTask(Sandalphon.getPlugin(), Runnable {
                                chest.open(player)
                            })
                        }
                    } else {
                        val pos = NMS.handle().fromBlockPosition(SimpleReflection.getFieldValueChecked(a.javaClass, a, "c", true))
                        val loc = Location(player.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                        val chest = TreasureChest.getChest(loc.block) ?: return true
                        if (packet.read("b").toString() == "MAIN_HAND") {
                            Bukkit.getScheduler().runTask(Sandalphon.getPlugin(), Runnable {
                                chest.open(player)
                            })
                        }
                    }
                    return false
                }
                return true
            }
        })
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun e(e: BlockBreakEvent) {
        if (TreasureChest.getChest(e.block) != null) {
            e.isCancelled = true
            e.player.info("你不能破坏宝藏.")
        }
    }

    @EventHandler
    fun e(e: InventoryCloseEvent) {
        if (e.inventory.holder is ChestInventory && e.inventory.viewers.size == 1) {
            val chest = (e.inventory.holder as ChestInventory).chestData
            e.inventory.filter { item -> Items.nonNull(item) }.forEachIndexed { index, item ->
                Bukkit.getScheduler().runTaskLater(Sandalphon.getPlugin(), Runnable {
                    CronusUtils.addItem(e.player as Player, item)
                    (e.player as Player).playSound(e.player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                }, index.toLong())
            }
            e.inventory.clear()
            chest.globalInventory = null
            chest.globalTime = System.currentTimeMillis() + chest.update
            chest.tick(e.player as Player, true)
            // closed animation
            if (chest.replace == Material.CHEST || chest.replace == Material.TRAPPED_CHEST) {
                e.player.world.players.forEach { p ->
                    ink.ptms.sandalphon.module.api.NMS.HANDLE.sendBlockAction(p, chest.block.block, 1, 0)
                }
                e.player.world.playSound(chest.block, Sound.BLOCK_CHEST_CLOSE, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
            }
        }
    }

    @EventHandler
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].unColored() == "TreasureChest") {
            val chestData = TreasureChest.getChest(Utils.toLocation(e.previousBookMeta.lore!![1].unColored()).block)
            if (chestData == null) {
                e.player.error("该宝藏已失效. (${e.previousBookMeta.lore!![1].unColored()})")
            } else {
                chestData.conditionText.clear()
                if (e.newBookMeta.pages[0].unColored() != "clear") {
                    chestData.conditionText.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                chestData.init()
            }
        }
    }
}