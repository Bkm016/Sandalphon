package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.util.item.Items
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.metadata.FixedMetadataValue

/**
 * @Author sky
 * @Since 2020-05-29 13:08
 */
@TListener(depend = ["MythicMobs"])
class SpawnerEvents : Listener, Helper {

    @EventHandler
    fun e(e: MythicMobDeathEvent) {
        Spawner.spawners.forEach { spawnerData ->
            spawnerData.mobs.forEach { (k, v) ->
                if (v.uniqueId == e.entity.uniqueId) {
                    spawnerData.time[k] = System.currentTimeMillis() + (spawnerData.respawn * 1000L)
                    e.entity.setMetadata("RESPAWN", FixedMetadataValue(Sandalphon.getPlugin(), spawnerData.respawn))
                }
            }
        }
    }

    @EventHandler
    fun e(e: BlockBreakEvent) {
        if (e.player.isOp && Items.hasName(e.player.inventory.itemInMainHand, "拷贝魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "Spawner")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![0].unColored())
            val spawnerData = Spawner.getSpawner(location.block)
            if (spawnerData == null) {
                e.player.error("该方块不存在刷怪箱.")
                return
            }
            if (e.block.location !in spawnerData.copy) {
                e.player.error("该方块不被拷贝.")
                return
            }
            e.block.display()
            e.player.info("移除刷怪箱拷贝.")
            spawnerData.block.block.display()
            spawnerData.copy.remove(e.block.location)
            spawnerData.cancel(e.block.location)
            Spawner.export()
        }
    }

    @EventHandler
    fun e(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        if (e.player.isOp && e.action == Action.RIGHT_CLICK_BLOCK && Items.hasName(e.player.inventory.itemInMainHand, "拷贝魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "Spawner")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![0].unColored())
            val spawnerData = Spawner.getSpawner(location.block)
            if (spawnerData == null) {
                e.player.error("该方块不存在刷怪箱.")
                return
            }
            if (e.clickedBlock!!.location in spawnerData.copy) {
                e.player.error("该方块已被拷贝.")
                return
            }
            e.clickedBlock!!.display()
            e.player.info("创建刷怪箱拷贝.")
            spawnerData.block.block.display()
            spawnerData.copy.add(e.clickedBlock!!.location)
            Spawner.export()
        }
    }
}