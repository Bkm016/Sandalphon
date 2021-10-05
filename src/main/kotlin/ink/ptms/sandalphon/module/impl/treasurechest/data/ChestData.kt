package ink.ptms.sandalphon.module.impl.treasurechest.data

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.treasurechest.TreasureChest
import ink.ptms.sandalphon.module.impl.treasurechest.event.ChestGenerateEvent
import ink.ptms.sandalphon.module.impl.treasurechest.event.ChestOpenEvent
import ink.ptms.sandalphon.util.Utils
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.kotlin.getLocalData
import io.izzel.taboolib.kotlin.sendHolographic
import io.izzel.taboolib.module.db.local.LocalPlayer
import io.izzel.taboolib.util.Times
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Effects
import io.izzel.taboolib.util.lite.Materials
import io.izzel.taboolib.util.lite.Numbers
import io.izzel.taboolib.util.lite.Signs
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.type.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.util.random
import taboolib.common5.Coerce
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage
import taboolib.module.nms.MinecraftVersion
import java.util.concurrent.CompletableFuture

/**
 * @author sky
 * @since 2020-05-29 21:39
 */
class ChestData(val block: Location) {

    var link: Location? = null
    val open = ArrayList<String>()
    val item = ArrayList<Pair<String, Int>>()

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
        val inventory = (block.block.state as org.bukkit.block.Chest).inventory
        if (inventory is DoubleChestInventory) {
            link = if (inventory.leftSide.location == block) {
                inventory.rightSide.location!!
            } else {
                inventory.leftSide.location!!
            }
        }
    }

    fun isChest(block: Block): Boolean {
        return if (isHighVersion) {
            block.blockData is Chest
        } else {
            block.state is DoubleChest
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
        val items = ArrayList<Pair<String, Int>>()
        if (random == -1 to -1) {
            items.addAll(item)
        } else if (item.isNotEmpty()) {
            val random = random(random.first, random.second.coerceAtMost(item.size).coerceAtLeast(random.first))
            item.toList().toMutableList().run {
                (1..random).forEach { _ ->
                    items.add(this.removeAt(random(this.size)))
                }
            }
        }
        items.forEach { (k, v) ->
            val item = ZaphkielAPI.getItem(k, player) ?: return@forEach
            val event = ChestGenerateEvent(player, this, item, v)
            event.call()
            if (!event.isCancelled) {
                inventory.setItem(content.removeAt(random(content.size)), event.item.save().run {
                    this.amount = event.amount
                    this
                })
            }
        }
        return inventory
    }

    fun tick() {
        if (open.isNotEmpty()) {
            block.world!!.players.forEach { NMS.instance.sendBlockAction(it, block.block, 1, 1) }
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
            val data = LocalPlayer.get(player)
            val time = data.getLong("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}")
            if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                if (particle) {
                    player.playSound(block, Sound.BLOCK_WOOD_BREAK, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
                    Effects.create(Particle.CRIT, block.clone().add(0.5, 0.5, 0.5)).speed(0.5).count(50).player(player).play()
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

    fun open(player: Player) {
        if (ChestOpenEvent(player, this).call().isCancelled) {
            player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
            return
        }
        if (TreasureChest.isGuardianNearly(block)) {
            player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f这个箱子正在被监视.")
            player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
            return
        }
        check(player).thenAccept { cond ->
            if (!cond) {
                player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f你无法打开这个箱子.")
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                return@thenAccept
            }
            if (locked != "null") {
                if (Items.isNull(player.inventory.itemInMainHand)) {
                    player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f需要钥匙才可以打开.")
                    player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                    return@thenAccept
                }
                if (Utils.asgardHook) {
                    if (!Items.hasLore(player.inventory.itemInMainHand, locked)) {
                        player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f需要钥匙才可以打开.")
                        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                        return@thenAccept
                    }
                } else {
                    val itemStream = ZaphkielAPI.read(player.inventory.itemInMainHand)
                    if (itemStream.isVanilla()) {
                        player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f需要钥匙才可以打开.")
                        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                        return@thenAccept
                    }
                    val compound = itemStream.getZaphkielData()["treasurechest"]
                    if (compound == null || (compound.asString() != locked && compound.asString() != "all")) {
                        player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f需要钥匙才可以打开.")
                        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                        return@thenAccept
                    }
                }
            }
            if (global) {
                if (globalTime > System.currentTimeMillis() || (update == -1L && globalTime > 0)) {
                    if (replace == Material.CHEST || replace == Material.TRAPPED_CHEST) {
                        player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f这个箱子什么都没有.")
                        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                    }
                    return@thenAccept
                }
                if (globalInventory == null) {
                    globalInventory = update(player, Bukkit.createInventory(ChestInventory(this), if (link != null) 54 else 27, title))
                }
                if (locked != "null") {
                    player.inventory.itemInMainHand.amount -= 1
                }
                player.openInventory(globalInventory!!)
            } else {
                val data = LocalPlayer.get(player)
                val time = data.getLong("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}")
                if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                    if (replace == Material.CHEST || replace == Material.TRAPPED_CHEST) {
                        player.sendHolographic(block.clone().add(0.5, 1.0, 0.5), "&c&l:(", "&f这个箱子什么都没有.")
                        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, Numbers.getRandomDouble(1.5, 2.0).toFloat())
                    }
                    return@thenAccept
                }
                if (locked != "null") {
                    player.inventory.itemInMainHand.amount -= 1
                }
                MenuBuilder.builder(Sandalphon.plugin)
                    .title(title)
                    .rows(if (link != null) 6 else 3)
                    .build {
                        update(player, it)
                        open.add(player.name)
                    }.close {
                        it.inventory.filter { item -> Items.nonNull(item) }.forEachIndexed { index, item ->
                            Bukkit.getScheduler().runTaskLater(Sandalphon.plugin, Runnable {
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
            if (isChest(block.block)) {
                player.world.players.forEach {
                    NMS.HANDLE.sendBlockAction(it, block.block, 1, 1)
                }
                player.world.playSound(block, Sound.BLOCK_CHEST_OPEN, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
            } else {
                player.world.playSound(block, Sound.BLOCK_CHEST_OPEN, 1f, Numbers.getRandomDouble(0.8, 1.2).toFloat())
            }
        }
    }

    fun openEdit(player: Player) {
        fun toGlobal() = ItemBuilder(Materials.BEACON.parseMaterial()).name("§f应用全局").lore(if (global) "§a启用" else "§c禁用").build()
        fun replaceBlock() = ItemBuilder(Materials.GLASS.parseMaterial()).name("§f替换方块").lore("§7${Items.getName(ItemStack(replace))}").build()
        MenuBuilder.builder()
            .title("编辑宝藏 ${Utils.fromLocation(block)}")
            .rows(4)
            .build { inv ->
                inv.setItem(10, ItemBuilder(Materials.NAME_TAG.parseMaterial()).name("§f名称").lore("§7$title").build())
                inv.setItem(11, ItemBuilder(Materials.TRIPWIRE_HOOK.parseMaterial()).name("§f钥匙").lore("§7${if (locked == "null") "无" else locked}").build())
                inv.setItem(12, ItemBuilder(Materials.HOPPER_MINECART.parseMaterial()).name("§f随机数量").lore("§7${random.first} -> ${random.second}").build())
                inv.setItem(13, ItemBuilder(Materials.CHEST_MINECART.parseMaterial()).name("§f刷新时间").lore("§7${getTimeDisplay(update)}").build())
                inv.setItem(14, toGlobal())
                inv.setItem(15, replaceBlock())
                inv.setItem(16, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f开启条件").lore(condition.map { "§7$it" }).build())
                inv.setItem(19, ItemBuilder(Materials.CHEST.parseMaterial()).name("§f编辑内容").build())
                inv.setItem(20, ItemBuilder(Materials.WATER_BUCKET.parseMaterial()).name("§f重置冷却").lore(if (global) "§7全局" else "§c个人").build())
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
                        it.currentItem = toGlobal()
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    15 -> {
                        if (player.inventory.itemInMainHand.type.isBlock) {
                            replace = player.inventory.itemInMainHand.type
                            it.currentItem = replaceBlock()
                            it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                        } else {
                            player.sendMessage("§c[Sandalphon] §7手持物品非方块.")
                        }
                    }
                    16 -> {
                        player.closeInventory()
                        CronusUtils.addItem(
                            player,
                            ItemBuilder(BookBuilder(ItemStack(Material.WRITABLE_BOOK)).pagesRaw(condition.joinToString("\n")).build())
                                .name("§f§f§f编辑条件")
                                .lore("§7TreasureChest", "§7${Utils.fromLocation(block)}")
                                .build()
                        )
                    }
                    19 -> {
                        openEditContent(player)
                    }
                    20 -> {
                        if (global) {
                            globalTime = 0
                        } else {
                            player.getLocalData().set("Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}", null)
                        }
                    }
                }
            }.close {
                TreasureChest.export()
            }.open(player)
    }

    fun openEditContent(player: Player) {
        MenuBuilder.builder(Sandalphon.plugin)
            .title("编辑宝藏内容 ${Utils.fromLocation(block)}")
            .rows(if (link != null) 6 else 3)
            .build {
                item.forEachIndexed { i, p ->
                    val itemStack = Utils.item(p.first, player) ?: return@forEachIndexed
                    it.setItem(i, itemStack.run {
                        this.amount = p.second
                        this
                    })
                }
            }.close {
                this.item.clear()
                it.inventory.filter { item -> Items.nonNull(item) }.forEach { item ->
                    val itemId = Utils.itemId(item)
                    if (itemId != null) {
                        this.item.add(itemId to item.amount)
                    }
                }
                TreasureChest.export()
            }.open(player)
    }

    fun getTimeDisplay(from: Long): String {
        val t = Times(from)
        val time =
            (if (t.days > 0) t.days.toString() + "天" else "") + (if (t.hours > 0) t.hours.toString() + "时" else "") + (if (t.minutes > 0) t.minutes.toString() + "分" else "") + if (t.seconds > 0) t.seconds.toString() + "秒" else ""
        return if (time.isEmpty()) "无" else time
    }

    fun getTimeFormatted(from: Long): String {
        val t = Times(from)
        val time =
            (if (t.days > 0) t.days.toString() + "d" else "") + (if (t.hours > 0) t.hours.toString() + "h" else "") + (if (t.minutes > 0) t.minutes.toString() + "m" else "") + if (t.seconds > 0) t.seconds.toString() + "s" else ""
        return if (time.isEmpty()) "-1" else time
    }
}