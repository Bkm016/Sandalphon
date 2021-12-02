package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestInventory
import ink.ptms.sandalphon.util.Utils
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.util.NumberConversions
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.submit
import taboolib.library.xseries.parseToMaterial
import taboolib.module.configuration.createLocal
import taboolib.platform.util.giveItem
import taboolib.platform.util.isNotAir

object TreasureChest {

    val data by lazy { createLocal("module/treasurechest.yml") }
    val chests = ArrayList<ChestData>()

    val isMythicMobsHooked by lazy {
        Bukkit.getPluginManager().getPlugin("MythicMobs") != null
    }

    @Awake(LifeCycle.ACTIVE)
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
            return
        }
        if (Bukkit.getPluginManager().getPlugin("Adyeshach") == null) {
            return
        }
        chests.clear()
        data.getKeys(false).forEach {
            chests.add(ChestData(Utils.toLocation(it.replace("__", "."))).run {
                items.addAll(data.getStringList("$it.item").map { item -> item.split(" ")[0] to NumberConversions.toInt(item.split(" ")[1]) })
                if (data.contains("$it.link")) {
                    link = Utils.toLocation(data.getString("$it.link")!!.replace("__", "."))
                }
                title = data.getString("$it.title")!!
                random = data.getInt("$it.random.min") to data.getInt("$it.random.max")
                update = data.getLong("$it.update")
                locked = data.getString("$it.locked")!!
                global = data.getBoolean("$it.global")
                globalTime = data.getLong("$it.global-time")
                replace = data.getString("$it.replace")!!.parseToMaterial()
                condition.addAll(data.getStringList("$it.condition"))
                this
            })
        }
    }

    @Schedule(period = 20 * 60, async = true)
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        chests.forEach { chest ->
            val location = Utils.fromLocation(chest.block).replace(".", "__")
            if (chest.link != null) {
                data.set("$location.link", Utils.fromLocation(chest.link!!).replace(".", "__"))
            }
            data.set("$location.item", chest.items.map { "${it.first} ${it.second}" })
            data.set("$location.title", chest.title)
            data.set("$location.random.min", chest.random.first)
            data.set("$location.random.max", chest.random.second)
            data.set("$location.update", chest.update)
            data.set("$location.locked", chest.locked)
            data.set("$location.global", chest.global)
            data.set("$location.global-time", chest.globalTime)
            data.set("$location.replace", chest.replace.name)
            data.set("$location.condition", chest.condition)
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun cancel() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val inventory = player.openInventory.topInventory
            if (inventory.holder is ChestInventory) {
                val chest = (inventory.holder as ChestInventory).chestData
                inventory.filter { item -> item.isNotAir() }.forEachIndexed { index, item ->
                    submit(delay = index.toLong()) {
                        player.giveItem(item)
                        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                    }
                }
                inventory.clear()
                chest.globalInventory = null
                chest.globalTime = System.currentTimeMillis() + chest.update
                player.closeInventory()
                NMS.INSTANCE.sendBlockAction(player, chest.block.block, 1, 0)
            }
        }
        export()
    }

    @Schedule(period = 20)
    fun tick() {
        chests.forEach { it.tick() }
    }

    fun delete(location: String) {
        ScriptBlock.data.set(location.replace(".", "__"), null)
    }

    fun getChest(block: Block): ChestData? {
        return chests.firstOrNull { it.isBlock(block) }
    }

    fun isGuardianNearly(loc: Location): Boolean {
        if (isMythicMobsHooked) {
            return loc.world!!.getNearbyEntities(loc, 16.0, 16.0, 16.0).any {
                if (it is LivingEntity) {
                    val mob = MythicMobs.inst().mobManager.getMythicMobInstance(it)
                    mob != null && mob.type.config.getStringList("Purtmars.Type").contains("guardian:treasure")
                } else {
                    false
                }
            }
        }
        return false
    }
}