package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestInventory
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.util.item.Items
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.type.Chest
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions
import org.spigotmc.AsyncCatcher

object TreasureChest {

    @LocalFile("module/treasurechest.yml")
    lateinit var data: FileConfiguration
        private set

    val chests = ArrayList<ChestData>()

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
            return
        }
        chests.clear()
        data.getKeys(false).forEach {
            chests.add(ChestData(Utils.toLocation(it.replace("__", "."))).run {
                this.item.addAll(data.getStringList("$it.item").map { item -> item.split(" ")[0] to NumberConversions.toInt(item.split(" ")[1]) })
                this.title = data.getString("$it.title")!!
                this.random = data.getInt("$it.random.min") to data.getInt("$it.random.max")
                this.update = data.getLong("$it.update")
                this.locked = data.getString("$it.locked")!!
                this.global = data.getBoolean("$it.global")
                this.globalTime = data.getLong("$it.global-time")
                this.replace = Items.asMaterial(data.getString("$it.replace"))
                this.conditionText.addAll(data.getStringList("$it.condition"))
                this
            })
        }
    }

    @TSchedule(period = 20 * 60)
    fun export() {
        chests.forEach { chest ->
            val location = Utils.fromLocation(chest.block).replace(".", "__")
            data.set("$location.item", chest.item.map { "${it.first} ${it.second}" })
            data.set("$location.title", chest.title)
            data.set("$location.random.min", chest.random.first)
            data.set("$location.random.max", chest.random.second)
            data.set("$location.update", chest.update)
            data.set("$location.locked", chest.locked)
            data.set("$location.global", chest.global)
            data.set("$location.global-time", chest.globalTime)
            data.set("$location.replace", chest.replace.name)
            data.set("$location.condition", chest.conditionText)
        }
    }

    @TFunction.Cancel
    fun cancel() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val inventory = player.openInventory.topInventory
            if (inventory.holder is ChestInventory) {
                val chest = (inventory.holder as ChestInventory).chestData
                inventory.filter { item -> Items.nonNull(item) }.forEach { item ->
                    CronusUtils.addItem(player as Player, item)
                }
                inventory.clear()
                chest.globalInventory = null
                chest.globalTime = System.currentTimeMillis() + chest.update
                player.closeInventory()
            }
        }
        export()
    }

    @TSchedule(period = 20)
    fun tick() {
        chests.forEach { it.tick() }
    }

    fun delete(location: String) {
        ScriptBlock.data.set(location.replace(".", "__"), null)
    }

    fun getChest(block: Block): ChestData? {
        return chests.firstOrNull { it.isBlock(block) }
    }
}