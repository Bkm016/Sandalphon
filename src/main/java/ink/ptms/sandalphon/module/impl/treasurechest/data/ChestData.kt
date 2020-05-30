package ink.ptms.sandalphon.module.impl.treasurechest.data

import ink.ptms.cronus.internal.condition.Condition
import ink.ptms.cronus.internal.condition.ConditionParser
import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.treasurechest.TreasureChest
import ink.ptms.sandalphon.module.impl.treasurechest.event.ItemGenerateEvent
import ink.ptms.sandalphon.util.Utils
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.db.local.LocalPlayer
import io.izzel.taboolib.util.Times
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Effects
import io.izzel.taboolib.util.lite.Numbers
import io.izzel.taboolib.util.lite.Signs
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.type.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.all
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.toList
import kotlin.collections.toMutableList

/**
 * @Author sky
 * @Since 2020-05-29 21:39
 */
class ChestData(val block: Location) {

    val open = ArrayList<String>()
    val link = ArrayList<Location>()
    val item = ArrayList<Pair<String, Int>>()

    var title = "箱子"
    var random = -1 to -1
    var update = -1L
    var locked = "null"
    var global = false
    var globalTime = 0L
    var globalInventory: Inventory? = null

    var replace = block.block.type

    val condition = ArrayList<Condition>()
    val conditionText = ArrayList<String>()

    init {
        if (block.block.blockData is Chest) {
            when ((block.block.blockData as Chest).type) {
                Chest.Type.LEFT -> link.add((block.block.state as DoubleChest).rightSide!!.inventory.location!!)
                Chest.Type.RIGHT -> link.add((block.block.state as DoubleChest).leftSide!!.inventory.location!!)
                else -> {
                }
            }
        }
    }

    fun isBlock(block: Block): Boolean {
        return this.block == block.location || block.location in link
    }

    fun init() {
        condition.clear()
        condition.addAll(conditionText.map { ConditionParser.parse(it) })
    }

    fun check(player: Player): Boolean {
        return condition.all { it.check(player) }
    }

    fun update(player: Player, inventory: Inventory): Inventory {
        val content: MutableList<Int> = IntStream.range(0, inventory.size).boxed().collect(Collectors.toList())!!
        val items = ArrayList<Pair<String, Int>>()
        if (random == -1 to -1) {
            items.addAll(item)
        } else {
            val random = Numbers.getRandomInteger(random.first, random.second.coerceAtLeast(item.size).coerceAtLeast(random.first))
            item.toList().toMutableList().run {
                (1..random).forEach { _ ->
                    items.add(this.removeAt(Numbers.getRandom().nextInt(this.size)))
                }
            }
        }
        items.forEach { (k, v) ->
            val item = ZaphkielAPI.getItem(k, player) ?: return@forEach
            val event = ItemGenerateEvent(player, this, item, v).call()
            if (event.nonCancelled()) {
                inventory.setItem(content.removeAt(Numbers.getRandom().nextInt(content.size)), event.item.rebuild(player).run {
                    this.amount = event.amount
                    this
                })
            }
        }
        return inventory
    }

    fun tick() {
        if (open.isNotEmpty()) {
            block.world!!.players.forEach { NMS.HANDLE.sendBlockAction(it, block.block, 1, 1) }
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
                    player.playSound(block, Sound.BLOCK_WOOD_BREAK, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
                    Effects.create(Particle.CRIT, block.clone().add(0.5, 0.5, 0.5)).speed(0.5).count(50).player(player).play()
                }
                player.sendBlockChange(block, replace.createBlockData())
            } else {
                player.sendBlockChange(block, block.block.blockData)
            }
        } else {
            val data = LocalPlayer.get(player)
            val time = data.getLong("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}")
            if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                if (particle) {
                    player.playSound(block, Sound.BLOCK_WOOD_BREAK, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
                    Effects.create(Particle.CRIT, block.clone().add(0.5, 0.5, 0.5)).speed(0.5).count(50).player(player).play()
                }
                player.sendBlockChange(block, replace.createBlockData())
            } else {
                player.sendBlockChange(block, block.block.blockData)
            }
        }
    }

    fun open(player: Player) {
        if (!check(player)) {
            player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
            return
        }
        if (locked != "null") {
            if (Items.isNull(player.inventory.itemInMainHand)) {
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                return
            }
            val itemStream = ZaphkielAPI.read(player.inventory.itemInMainHand)
            if (itemStream.isVanilla()) {
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                return
            }
            val compound = itemStream.getZaphkielData()["treasurechest"]
            if (compound == null || (compound.asString() != locked && compound.asString() != "all")) {
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                return
            }
            player.inventory.itemInMainHand.amount -= 1
        }
        if (global) {
            if (globalTime > System.currentTimeMillis()) {
                if (replace == Material.CHEST || replace == Material.TRAPPED_CHEST) {
                    player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                }
                return
            }
            if (globalInventory == null) {
                globalInventory = Bukkit.createInventory(ChestInventory(this), if (link.isNotEmpty()) 54 else 27, title)
            }
            player.openInventory(update(player, globalInventory!!))
        } else {
            val data = LocalPlayer.get(player)
            val time = data.getLong("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}")
            if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                if (replace == Material.CHEST || replace == Material.TRAPPED_CHEST) {
                    player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                }
                return
            }
            MenuBuilder.builder(Sandalphon.getPlugin())
                    .title(title)
                    .rows(if (link.isNotEmpty()) 6 else 3)
                    .build {
                        update(player, it)
                        open.add(player.name)
                    }.close {
                        it.inventory.filter { item -> Items.nonNull(item) }.forEachIndexed { index, item ->
                            Bukkit.getScheduler().runTaskLater(Sandalphon.getPlugin(), Runnable {
                                CronusUtils.addItem(player, item)
                                player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                            }, index.toLong())
                        }
                        it.inventory.clear()
                        data.set("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}", System.currentTimeMillis() + update)
                        tick(player, true)
                        open.remove(player.name)
                        // closed animation
                        if (open.isEmpty() && (replace == Material.CHEST || replace == Material.TRAPPED_CHEST)) {
                            player.world.players.forEach { p ->
                                NMS.HANDLE.sendBlockAction(p, block.block, 1, 0)
                            }
                            player.world.playSound(block, Sound.BLOCK_CHEST_CLOSE, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
                        }
                    }.open(player)
        }
        if (block.block.blockData is Chest) {
            player.world.players.forEach { p ->
                NMS.HANDLE.sendBlockAction(p, block.block, 1, 1)
            }
            player.world.playSound(block, Sound.BLOCK_CHEST_OPEN, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
        } else {
            player.playSound(block, Sound.BLOCK_CHEST_OPEN, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
        }
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑宝藏 ${Utils.fromLocation(block)}")
                .rows(3)
                .build { inv ->
                    inv.setItem(10, ItemBuilder(Material.NAME_TAG).name("§f名称").lore("§7$title").build())
                    inv.setItem(11, ItemBuilder(Material.TRIPWIRE_HOOK).name("§f钥匙").lore("§7${if (locked == "null") "无" else locked}").build())
                    inv.setItem(12, ItemBuilder(Material.HOPPER_MINECART).name("§f随机").lore("§7${random.first} -> ${random.second}").build())
                    inv.setItem(13, ItemBuilder(Material.CHEST_MINECART).name("§f刷新").lore("§7${getTimeDisplay(update)}").build())
                    inv.setItem(14, ItemBuilder(Material.BEACON).name("§f全局").lore(if (global) "§a启用" else "§c禁用").build())
                    inv.setItem(15, ItemBuilder(Material.GLASS).name("§f替换").lore("§7${Items.getName(ItemStack(replace))}").build())
                    inv.setItem(16, ItemBuilder(Material.OBSERVER).name("§f条件").lore(conditionText.map { "§7$it" }).build())
                }.event {
                    it.isCancelled = true
                    when (it.rawSlot) {
                        10 -> {
                            Signs.fakeSign(player, arrayOf(title)) { sign ->
                                title = sign[0]
                                openEdit(player)
                            }
                        }
                        11 -> {
                            Signs.fakeSign(player, arrayOf(locked)) { sign ->
                                locked = sign[0]
                                openEdit(player)
                            }
                        }
                        12 -> {
                            Signs.fakeSign(player, arrayOf(random.first.toString(), random.second.toString())) { sign ->
                                random = NumberConversions.toInt(sign[0]) to NumberConversions.toInt(sign[1])
                                openEdit(player)
                            }
                        }
                        13 -> {
                            Signs.fakeSign(player, arrayOf(getTimeFormatted(update))) { sign ->
                                update = CronusUtils.toMillis(sign[0])
                                openEdit(player)
                            }
                        }
                        14 -> {
                            global = !global
                            it.currentItem = ItemBuilder(Material.BEACON).name("§f全局").lore(if (global) "§a启用" else "§c禁用").build()
                        }
                        15 -> {
                            if (player.inventory.itemInMainHand.type.isBlock || player.inventory.itemInMainHand.type == Material.AIR) {
                                replace = player.inventory.itemInMainHand.type
                                it.inventory.setItem(15, ItemBuilder(Material.GLASS).name("§f替换").lore("§7${Items.getName(ItemStack(replace))}").build())
                            } else {
                                player.sendMessage("§c[Sandalphon] §7手持物品非方块.")
                            }
                        }
                        16 -> {
                            player.closeInventory()
                            CronusUtils.addItem(player, ItemBuilder(BookBuilder(ItemStack(Material.WRITABLE_BOOK)).pagesRaw(conditionText.joinToString("\n")).build()).name("§f§f§f编辑条件").lore("§7TreasureChestw", "§7${Utils.fromLocation(block)}").build())
                        }
                    }
                }.close {
                    TreasureChest.export()
                    init()
                }.open(player)
    }

    fun openEditContent(player: Player) {
        MenuBuilder.builder(Sandalphon.getPlugin())
                .title("编辑宝藏内容 ${Utils.fromLocation(block)}")
                .rows(if (link.isNotEmpty()) 6 else 3)
                .build {
                    item.forEach { (k, v) ->
                        val itemStream = ZaphkielAPI.getItem(k, player) ?: return@forEach
                        it.addItem(itemStream.rebuild(player).run {
                            this.amount = v
                            this
                        })
                    }
                }.close {
                    this.item.clear()
                    it.inventory.filter { item -> Items.nonNull(item) }.forEach { item ->
                        val itemStream = ZaphkielAPI.read(item)
                        if (itemStream.isExtension()) {
                            this.item.add(itemStream.getZaphkielName() to item.amount)
                        }
                    }
                    TreasureChest.export()
                    init()
                }.open(player)
    }

    fun getTimeDisplay(from: Long): String {
        val t = Times(from)
        val time = (if (t.days > 0) t.days.toString() + "天" else "") + (if (t.hours > 0) t.hours.toString() + "时" else "") + (if (t.minutes > 0) t.minutes.toString() + "分" else "") + if (t.seconds > 0) t.seconds.toString() + "秒" else ""
        return if (time.isEmpty()) "无" else time
    }

    fun getTimeFormatted(from: Long): String {
        val t = Times(from)
        val time = (if (t.days > 0) t.days.toString() + "d" else "") + (if (t.hours > 0) t.hours.toString() + "h" else "") + (if (t.minutes > 0) t.minutes.toString() + "m" else "") + if (t.seconds > 0) t.seconds.toString() + "s" else ""
        return if (time.isEmpty()) "-1" else time
    }
}