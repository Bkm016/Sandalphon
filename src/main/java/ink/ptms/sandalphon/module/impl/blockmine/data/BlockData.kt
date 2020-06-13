package ink.ptms.sandalphon.module.impl.blockmine.data

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.blockmine.BlockMine
import ink.ptms.sandalphon.module.impl.blockmine.event.BlockGrowEvent
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.internal.gson.annotations.Expose
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.Items
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Effects
import io.izzel.taboolib.util.lite.Numbers
import io.izzel.taboolib.util.lite.Signs
import org.bukkit.*
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions

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
    val blocks = ArrayList<BlockState>()

    fun find(block: Location): Pair<BlockState, BlockStructure>? {
        for (state in blocks) {
            if (state.location.world?.name == block.world?.name && state.location.distance(block) < 50) {
                for (structure in progress[state.current].structures) {
                    if (state.location.clone().add(structure.offset) == block) {
                        return state to structure
                    }
                }
            }
        }
        return null
    }

    fun isBroken(blockState: BlockState): Boolean {
        return progress[blockState.current].structures.all { blockState.location.clone().add(it.offset).block.type != it.origin }
    }

    fun clean(blockState: BlockState) {
        progress[blockState.current].structures.forEach { blockState.location.clone().add(it.offset).block.type = it.replace }
    }

    fun build(blockState: BlockState) {
        progress[blockState.current].structures.forEach {
            val block = blockState.location.clone().add(it.offset).block.run {
                if (this.type == it.origin) {
                    return@forEach
                }
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
            Bukkit.getScheduler().runTaskAsynchronously(Sandalphon.getPlugin(), Runnable {
                Effects.create(Particle.EXPLOSION_NORMAL, block.location.add(0.5, 0.5, 0.5)).count(5).offset(doubleArrayOf(0.5, 0.5, 0.5)).range(50.0).play()
            })
        }
    }

    fun grow(force: Boolean = false) {
        for (blockState in blocks) {
            if (grow(blockState, force)) {
                return
            }
        }
    }

    fun grow(blockState: BlockState, force: Boolean = false): Boolean {
        if (BlockGrowEvent(this, blockState).call().isCancelled) {
            return false
        }
        if (!force && (!blockState.update && blockState.current + 1 == progress.size)) {
            blockState.latest = System.currentTimeMillis()
            return false
        }
        if (!force && System.currentTimeMillis() - blockState.latest < (growTime * 1000L)) {
            return false
        }
        if (!force && Numbers.random(growChange)) {
            if (isBroken(blockState)) {
                blockState.current = 0
            } else if (!blockState.update) {
                clean(blockState)
                blockState.current = if (blockState.current + 1 == progress.size) 0 else blockState.current + 1
            }
            build(blockState)
            blockState.update = false
        }
        blockState.latest = System.currentTimeMillis()
        return true
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑开采结构 $id")
                .rows(3)
                .build { inv ->
                    inv.setItem(12, ItemBuilder(Material.STRUCTURE_VOID).name("§f阶段 (${progress.size})").lore(progress.mapIndexed { index, progress ->
                        "§7第 ${index + 1} 阶段包含 ${progress.structures.size} 个结构"
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
                            openEditProgress(player)
                            it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                        }
                        14 -> {
                            if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                Signs.fakeSign(player, arrayOf("$growTime")) { sign ->
                                    growTime = NumberConversions.toInt(sign[0])
                                    openEdit(player)
                                }
                            } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                Signs.fakeSign(player, arrayOf("$growChange")) { sign ->
                                    growChange = NumberConversions.toDouble(sign[0])
                                    openEdit(player)
                                }
                            }
                        }
                    }
                }.close {
                    BlockMine.export()
                }.open(player)
    }

    fun openEditProgress(player: Player, openProgress: BlockProgress? = null, openStructure: BlockStructure? = null) {
        when {
            openProgress == null -> {
                MenuBuilder.builder()
                        .title("编辑开采结构 $id")
                        .rows(3)
                        .build { inv ->
                            progress.forEachIndexed { index, progress -> inv.addItem(ItemBuilder(Material.PAPER).name("§f阶段 (${index})").lore("§7包含 ${progress.structures.size} 个结构", "", "§8左键编辑", "§8右键捕获", "§c丢弃删除").build()) }
                            inv.addItem(ItemBuilder(Material.MAP).name("§f阶段 (+)").lore("§7新增阶段").build())
                        }.event {
                            it.isCancelled = true
                            if (it.rawSlot == -999) {
                                openEdit(player)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            if (it.rawSlot >= 0 && it.rawSlot < progress.size) {
                                if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                    openEditProgress(player, progress[it.rawSlot])
                                } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                    player.sendMessage("§c[Sandalphon] §7使用§f捕获魔杖§7左键选取起点, 右键选取终点, 丢弃完成捕获.")
                                    player.inventory.addItem(ItemBuilder(Material.BLAZE_ROD).name("§f§f§f捕获魔杖").lore("§7BlockMine", "§7${id} ${it.rawSlot}").build())
                                    player.closeInventory()
                                } else if (it.clickType == ClickType.CLICK && it.castClick().click == org.bukkit.event.inventory.ClickType.DROP) {
                                    progress.removeAt(it.rawSlot)
                                    openEditProgress(player)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            } else if (it.rawSlot == progress.size) {
                                progress.add(BlockProgress(ArrayList()))
                                openEditProgress(player)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                        }.close {
                            BlockMine.export()
                        }.open(player)
            }
            openStructure == null -> {
                val structureMap = HashMap<Material, BlockStructure>()
                MenuBuilder.builder()
                        .title("编辑开采结构 $id")
                        .rows(3)
                        .build { inv ->
                            openProgress.structures.forEach { structure -> structureMap[structure.origin] = structure }
                            structureMap.forEach { (k, v) -> inv.addItem(ItemBuilder(k).lore("§7替换: ${Items.getName(ItemStack(v.replace))}", "§7工具: ${v.tool ?: "无"}", "§7掉落: ${v.drop.size} 项", "", "§8点击编辑").build()) }
                        }.event {
                            it.isCancelled = true
                            if (it.rawSlot == -999) {
                                openEditProgress(player)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            if (it.rawSlot in 0..26 && Items.nonNull(it.currentItem)) {
                                val structure = structureMap[it.currentItem.type]
                                if (structure != null) {
                                    openEditProgress(player, openProgress, structure)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                            }
                        }.open(player)
            }
            else -> {
                MenuBuilder.builder()
                        .title("编辑开采结构 $id")
                        .rows(3)
                        .build { inv ->
                            inv.setItem(11, ItemBuilder(Material.GLASS).name("§f替换").lore("§7${Items.getName(ItemStack(openStructure.replace))}").build())
                            inv.setItem(13, ItemBuilder(Material.IRON_PICKAXE).name("§f工具").lore("§7${openStructure.tool ?: "无"}").build())
                            inv.setItem(15, ItemBuilder(Material.CHEST_MINECART).name("§f掉落").lore(openStructure.drop.map { "§7${it.item} * ${it.amount} (${it.chance * 100}%)" }).build())
                        }.event {
                            it.isCancelled = true
                            when (it.rawSlot) {
                                11 -> {
                                    openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                        structure.replace = player.inventory.itemInMainHand.type
                                    }
                                    it.inventory.setItem(11, ItemBuilder(Material.GLASS).name("§f替换").lore("§7${Items.getName(ItemStack(openStructure.replace))}").build())
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                                13 -> {
                                    Signs.fakeSign(player, arrayOf(openStructure.tool ?: "")) { sign ->
                                        openStructure.tool = sign[0]
                                        openEditProgress(player, openProgress, openStructure)
                                    }
                                }
                                15 -> {
                                    openEditDrop(player, openProgress, openStructure)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                                -999 -> {
                                    openEditProgress(player, openProgress)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                            }
                        }.close {
                            BlockMine.export()
                        }.open(player)
            }
        }
    }

    fun openEditDrop(player: Player, openProgress: BlockProgress, openStructure: BlockStructure) {
        MenuBuilder.builder()
                .title("编辑开采结构 $id")
                .rows(3)
                .build { inv ->
                    openStructure.drop.forEachIndexed { index, drop ->
                        val item = Utils.item(drop.item, player) ?: return@forEachIndexed
                        inv.setItem(index, ItemBuilder(item.type).name("§f${drop.item}").lore("§7${drop.chance * 100}%", "", "§7左键编辑", "§c丢弃删除").build())
                    }
                    inv.addItem(ItemBuilder(Material.MAP).name("§f掉落 (+)").lore("§7新增掉落").build())
                }.event {
                    it.isCancelled = true
                    if (it.rawSlot == -999) {
                        openEditProgress(player, openProgress, openStructure)
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    if (it.rawSlot >= 0 && it.rawSlot < openStructure.drop.size) {
                        if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                            Signs.fakeSign(player, arrayOf("${openStructure.drop[it.rawSlot].chance}")) { sign ->
                                openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                    structure.drop[it.rawSlot].chance = NumberConversions.toDouble(sign[0])
                                }
                                openEditDrop(player, openProgress, openStructure)
                            }
                        } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                            openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                structure.drop.removeAt(it.rawSlot)
                            }
                            openEditDrop(player, openProgress, openStructure)
                        }
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    } else if (it.rawSlot == openStructure.drop.size) {
                        MenuBuilder.builder()
                                .title("编辑开采结构 $id")
                                .rows(3)
                                .close { close ->
                                    close.inventory.filter { item -> Items.nonNull(item) }.forEach { item ->
                                        val itemId = Utils.itemId(item)
                                        if (itemId != null) {
                                            openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                                structure.drop.add(BlockDrop(itemId, item.amount, 0.0))
                                            }
                                        }
                                    }
                                    BlockMine.export()
                                    Bukkit.getScheduler().runTaskLater(Sandalphon.getPlugin(), Runnable {
                                        openEditDrop(player, openProgress, openStructure)
                                    }, 1)
                                }.open(player)
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                }.close {
                    BlockMine.export()
                }.open(player)
    }
}