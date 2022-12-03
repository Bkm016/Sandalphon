@file:Suppress("DuplicatedCode", "SpellCheckingInspection")

package ink.ptms.sandalphon.module.impl.treasurechest.data

import ink.ptms.adyeshach.api.AdyeshachAPI
import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.treasurechest.TreasureChest
import ink.ptms.sandalphon.module.impl.treasurechest.event.ChestOpenEvent
import ink.ptms.sandalphon.module.impl.treasurechest.isTreasureType
import ink.ptms.sandalphon.util.ItemBuilder
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import taboolib.common.platform.function.submit
import taboolib.common.util.random
import taboolib.common5.Coerce
import taboolib.common5.util.parseMillis
import taboolib.expansion.getDataContainer
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.nms.getName
import taboolib.module.nms.inputSign
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildBook
import taboolib.platform.util.giveItem
import taboolib.platform.util.isAir
import taboolib.platform.util.isNotAir

fun ChestData.open(player: Player) {
    if (!ChestOpenEvent(player, this).call()) {
        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
        return
    }
    if (TreasureChest.isGuardianNearly(block)) {
        AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f这个箱子正在被监视.")
        player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
        return
    }
    check(player).thenAccept { cond ->
        if (!cond) {
            AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f你无法打开这个箱子.")
            player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
            return@thenAccept
        }
        if (locked != "null") {
            if (player.inventory.itemInMainHand.isAir()) {
                AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f需要钥匙才可以打开.")
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
                return@thenAccept
            }
            val keyId = Sandalphon.itemAPI!!.getData(player.inventory.itemInMainHand, "treasurechest")
            if (keyId == null || (keyId != locked && keyId != "all")) {
                AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f需要钥匙才可以打开.")
                player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
                return@thenAccept
            }
        }
        if (global) {
            if (globalTime > System.currentTimeMillis() || (update == -1L && globalTime > 0)) {
                if (replace.isTreasureType()) {
                    AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f这个箱子什么都没有.")
                    player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
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
            val data = player.getDataContainer()
            val time = Coerce.toLong(data["Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}"])
            if (time > System.currentTimeMillis() || (update == -1L && time > 0)) {
                if (replace.isTreasureType()) {
                    AdyeshachAPI.createHolographic(player, block.clone().add(0.5, 1.0, 0.5), "§c§l:(", "§f这个箱子什么都没有.")
                    player.playSound(block, Sound.BLOCK_CHEST_LOCKED, 1f, random(1.5, 2.0).toFloat())
                }
                return@thenAccept
            }
            if (locked != "null") {
                player.inventory.itemInMainHand.amount -= 1
            }
            player.openMenu<Basic>(title) {
                rows(if (link != null) 6 else 3)
                onBuild { _, inv ->
                    update(player, inv)
                    open.add(player.name)
                }
                onClose(once = false) {
                    it.inventory.filter { item -> item.isNotAir() }.forEachIndexed { index, item ->
                        submit(delay = index.toLong()) {
                            player.giveItem(item)
                            player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 2f)
                        }
                    }
                    it.inventory.clear()
                    data["Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}"] = System.currentTimeMillis() + update
                    tick(player, true)
                    open.remove(player.name)
                    // 关闭箱子的特效
                    player.world.players.forEach { p -> NMS.instance.sendBlockAction(p, block.block, 1, 0) }
                    player.world.playSound(block, Sound.BLOCK_CHEST_CLOSE, 1f, random(0.8, 1.2).toFloat())
                }
            }
        }
        // 打开箱子的特效
        player.world.players.forEach { NMS.instance.sendBlockAction(it, block.block, 1, 1) }
        player.world.playSound(block, Sound.BLOCK_CHEST_OPEN, 1f, random(0.8, 1.2).toFloat())
    }
}

fun ChestData.openEdit(player: Player) {
    fun toGlobal() = ItemBuilder(XMaterial.BEACON).name("§f应用全局").lore(if (global) "§a启用" else "§c禁用").build()
    fun replaceBlock() = ItemBuilder(XMaterial.GLASS).name("§f替换方块").lore("§7${ItemStack(replace).getName(player)}").build()
    player.openMenu<Basic>("编辑宝藏 ${Utils.fromLocation(block)}") {
        rows(4)
        onBuild { _, inv ->
            inv.setItem(10, ItemBuilder(XMaterial.NAME_TAG).name("§f名称").lore("§7$title").build())
            inv.setItem(11, ItemBuilder(XMaterial.TRIPWIRE_HOOK).name("§f钥匙").lore("§7${if (locked == "null") "无" else locked}").build())
            inv.setItem(12, ItemBuilder(XMaterial.HOPPER_MINECART).name("§f随机数量").lore("§7${random.first} -> ${random.second}").build())
            inv.setItem(13, ItemBuilder(XMaterial.CHEST_MINECART).name("§f刷新时间").lore("§7${getTimeDisplay(update)}").build())
            inv.setItem(14, toGlobal())
            inv.setItem(15, replaceBlock())
            inv.setItem(16, ItemBuilder(XMaterial.OBSERVER).name("§f开启条件").lore(condition.map { "§7$it" }).build())
            inv.setItem(19, ItemBuilder(XMaterial.CHEST).name("§f编辑内容").build())
            inv.setItem(20, ItemBuilder(XMaterial.WATER_BUCKET).name("§f重置冷却").lore(if (global) "§7全局" else "§c个人").build())
        }
        onClick(lock = true) {
            it.isCancelled = true
            when (it.rawSlot) {
                10 -> {
                    player.inputSign(arrayOf(this@openEdit.title)) { sign ->
                        this@openEdit.title = sign[0]
                        openEdit(player)
                    }
                }
                11 -> {
                    player.inputSign(arrayOf(locked)) { sign ->
                        locked = sign[0]
                        openEdit(player)
                    }
                }
                12 -> {
                    player.inputSign(arrayOf(random.first.toString(), random.second.toString())) { sign ->
                        random = NumberConversions.toInt(sign[0]) to NumberConversions.toInt(sign[1])
                        openEdit(player)
                    }
                }
                13 -> {
                    player.inputSign(arrayOf(getTimeFormatted(update))) { sign ->
                        update = sign[0].parseMillis()
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
                    player.giveItem(buildBook {
                        material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
                        write(condition.joinToString("\n"))
                        name = "§f§f§f编辑条件"
                        lore += listOf("§7TreasureChest", "§7${Utils.fromLocation(block)}")
                    })
                }
                19 -> {
                    openEditContent(player)
                }
                20 -> {
                    if (global) {
                        globalTime = 0
                    } else {
                        player.getDataContainer()["Sandalphon.treasurechest.${Utils.fromLocation(block).replace(".", "__")}"] = "0"
                    }
                    player.sendMessage("§c[Sandalphon] §7冷却已刷新.")
                }
            }
        }
        onClose(once = false) {
            TreasureChest.export()
        }
    }
}

fun ChestData.openEditContent(player: Player) {
    player.openMenu<Basic>("编辑宝藏内容 ${Utils.fromLocation(block)}") {
        rows(if (link != null) 6 else 3)
        onBuild { _, inv ->
            this@openEditContent.items.forEachIndexed { i, p ->
                val itemStack = Sandalphon.itemAPI!!.getItem(p.first, player) ?: return@forEachIndexed
                inv.setItem(i, itemStack.run {
                    this.amount = p.second
                    this
                })
            }
        }
        onClose(once = false) {
            this@openEditContent.items.clear()
            it.inventory.filter { i -> i.isNotAir() }.forEach { i ->
                val itemId = Sandalphon.itemAPI!!.getId(i)
                if (itemId != null) {
                    this@openEditContent.items.add(itemId to i.amount)
                }
            }
            TreasureChest.export()
        }
    }
}