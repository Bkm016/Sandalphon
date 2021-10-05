package ink.ptms.sandalphon.module.impl.blockmine.data

import com.google.gson.annotations.Expose
import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.api.NMS
import ink.ptms.sandalphon.module.impl.CommandBlockControl
import ink.ptms.sandalphon.module.impl.blockmine.BlockMine
import ink.ptms.sandalphon.module.impl.blockmine.event.BlockGrowEvent
import ink.ptms.sandalphon.util.Utils
import org.bukkit.*
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import taboolib.common.platform.function.submit
import taboolib.common.util.random
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.getI18nName
import taboolib.module.nms.inputSign
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildItem
import taboolib.platform.util.giveItem
import taboolib.platform.util.isNotAir

/**
 * @author sky
 * @since 2020-06-01 13:35
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
        return progress[blockState.current].structures.all {
            blockState.location.clone().add(it.offset).block.type != it.origin
        }
    }

    fun clean(blockState: BlockState) {
        progress[blockState.current].structures.forEach {
            blockState.location.clone().add(it.offset).block.type = it.replace
        }
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
            if (isAfter11300) {
                val blockData = block.blockData
                if (blockData is Directional) {
                    block.blockData = blockData.run {
                        this.facing = it.direction
                        this
                    }
                }
            } else {
                NMS.instance.setBlockData(block, CommandBlockControl.fromBlockFace(it.direction).toByte())
            }
            submit(async = true) {
                block.world.spawnParticle(Particle.EXPLOSION_NORMAL, block.location.add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0.0)
            }
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
        if (!BlockGrowEvent(this, blockState).call()) {
            return false
        }
        if (!force && (!blockState.update && blockState.current + 1 == progress.size)) {
            blockState.latest = System.currentTimeMillis()
            return false
        }
        if (!force && System.currentTimeMillis() - blockState.latest < (growTime * 1000L)) {
            return false
        }
        if (!force && random(growChange)) {
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
        player.openMenu<Basic>("编辑开采结构 $id") {
            rows(3)
            set(12, XMaterial.STRUCTURE_VOID) {
                name = "§f阶段 (${progress.size})"
                val list = progress.mapIndexed { index, progress -> "§7第 ${index + 1} 阶段包含 ${progress.structures.size} 个结构" }.toMutableList()
                list += ""
                list += "§8点击编辑"
                lore += list
            }
            set(14, XMaterial.BOOKSHELF) {
                name = "§f生长"
                lore += "§7时间: ${growTime}秒 §8(左键编辑)"
                lore += "§7几率: ${growChange * 100}% §8(右键编辑)"
            }
            onClick(12) {
                openEditProgress(player)
                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
            }
            onClick(14) {
                if (it.clickEvent().isLeftClick) {
                    player.inputSign(arrayOf("$growTime")) { sign ->
                        growTime = NumberConversions.toInt(sign[0])
                        openEdit(player)
                    }
                } else if (it.clickEvent().isRightClick) {
                    player.inputSign(arrayOf("$growChange")) { sign ->
                        growChange = NumberConversions.toDouble(sign[0])
                        openEdit(player)
                    }
                }
            }
            onClose {
                BlockMine.export()
            }
        }
    }

    fun openEditProgress(player: Player, openProgress: BlockProgress? = null, openStructure: BlockStructure? = null) {
        when {
            openProgress == null -> {
                player.openMenu<Basic>("编辑开采结构 $id") {
                    rows(3)
                    onBuild { _, inv ->
                        progress.forEachIndexed { index, progress ->
                            inv.addItem(buildItem(XMaterial.PAPER) {
                                name = "§f阶段 (${index})"
                                lore += arrayOf("§7包含 ${progress.structures.size} 个结构", "", "§8左键编辑", "§8右键捕获", "§c丢弃删除")
                            })
                        }
                        inv.addItem(buildItem(XMaterial.MAP) {
                            name = "§f阶段 (+)"
                            lore += "§7新增阶段"
                        })
                    }
                    onClick(lock = true) {
                        if (it.rawSlot == -999) {
                            openEdit(player)
                            it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                        }
                        if (it.rawSlot >= 0 && it.rawSlot < progress.size) {
                            when {
                                it.clickEvent().isLeftClick -> {
                                    openEditProgress(player, progress[it.rawSlot])
                                }
                                it.clickEvent().isRightClick -> {
                                    player.sendMessage("§c[Sandalphon] §7使用§f捕获魔杖§7左键选取起点, 右键选取终点, 丢弃完成捕获.")
                                    player.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                                        name = "§f§f§f捕获魔杖"
                                        lore += arrayOf("§7BlockMine", "§7${id} ${it.rawSlot}")
                                    })
                                    player.closeInventory()
                                }
                                it.clickEvent().click == org.bukkit.event.inventory.ClickType.DROP -> {
                                    progress.removeAt(it.rawSlot)
                                    openEditProgress(player)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                            }
                            it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                        } else if (it.rawSlot == progress.size) {
                            progress.add(BlockProgress(ArrayList()))
                            openEditProgress(player)
                            it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                        }
                    }
                    onClose {
                        BlockMine.export()
                    }
                }
            }
            openStructure == null -> {
                val structureMap = HashMap<Material, BlockStructure>()
                player.openMenu<Basic>("编辑开采结构 $id") {
                    rows(3)
                    onBuild { _, inv ->
                        openProgress.structures.forEach { structure -> structureMap[structure.origin] = structure }
                        structureMap.forEach { (k, v) ->
                            inv.addItem(buildItem(XMaterial.matchXMaterial(k)) {
                                lore += "§7替换: ${ItemStack(v.replace).getI18nName(player)}"
                                lore += "§7工具: ${v.tool ?: "无"}"
                                lore += "§7掉落: ${v.drop.size} 项"
                                lore += arrayOf("", "§8点击编辑")
                            })
                        }
                    }
                    onClick(lock = true) {
                        when {
                            it.rawSlot == -999 -> {
                                openEditProgress(player)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            it.rawSlot in 0..26 && it.currentItem.isNotAir() -> {
                                val structure = structureMap[it.currentItem!!.type]
                                if (structure != null) {
                                    openEditProgress(player, openProgress, structure)
                                    it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                player.openMenu<Basic>("编辑开采结构 $id") {
                    rows(3)
                    set(11, XMaterial.GLASS) {
                        name = "§f替换"
                        lore += "§7${ItemStack(openStructure.replace).getI18nName(player)}"
                    }
                    set(13, XMaterial.IRON_PICKAXE) {
                        name = "§f工具"
                        lore += "§7${openStructure.tool ?: "无"}"
                    }
                    set(15, XMaterial.CHEST_MINECART) {
                        name = "§f掉落"
                        lore += openStructure.drop.map { "§7${it.item} * ${it.amount} (${it.chance * 100}%)" }
                    }
                    onClick(lock = true) {
                        when (it.rawSlot) {
                            -999 -> {
                                openEditProgress(player, openProgress)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            11 -> {
                                openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                    structure.replace = player.inventory.itemInMainHand.type
                                }
                                it.inventory.setItem(11, buildItem(XMaterial.GLASS) {
                                    name = "§f替换"
                                    lore += "§7${ItemStack(openStructure.replace).getI18nName(player)}"
                                })
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            13 -> {
                                player.inputSign(arrayOf(openStructure.tool ?: "")) { sign ->
                                    openStructure.tool = sign[0]
                                    openEditProgress(player, openProgress, openStructure)
                                }
                            }
                            15 -> {
                                openEditDrop(player, openProgress, openStructure)
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                        }
                    }
                    onClose {
                        BlockMine.export()
                    }
                }
            }
        }
    }

    fun openEditDrop(player: Player, openProgress: BlockProgress, openStructure: BlockStructure) {
        player.openMenu<Basic>("编辑开采结构 $id") {
            rows(3)
            onBuild { _, inv ->
                openStructure.drop.forEachIndexed { index, drop ->
                    val item = Utils.item(drop.item, player) ?: return@forEachIndexed
                    inv.setItem(index, buildItem(item.type) {
                        name = "§f${drop.item}"
                        lore += arrayOf("§7${drop.chance * 100}%", "", "§7左键编辑", "§c丢弃删除")
                    })
                }
                inv.addItem(buildItem(XMaterial.MAP) {
                    name = "§f掉落 (+)"
                    lore += "§7新增掉落"
                })
            }
            onClick(lock = true) {
                when {
                    it.rawSlot == -999 -> {
                        openEditProgress(player, openProgress, openStructure)
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    it.rawSlot >= 0 && it.rawSlot < openStructure.drop.size -> {
                        when {
                            it.clickEvent().isLeftClick -> {
                                player.inputSign(arrayOf("${openStructure.drop[it.rawSlot].chance}")) { sign ->
                                    openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                        structure.drop[it.rawSlot].chance = NumberConversions.toDouble(sign[0])
                                    }
                                    openEditDrop(player, openProgress, openStructure)
                                }
                            }
                            it.clickEvent().isRightClick -> {
                                openProgress.structures.filter { structure -> structure.origin == openStructure.origin }.forEach { structure ->
                                    structure.drop.removeAt(it.rawSlot)
                                }
                                openEditDrop(player, openProgress, openStructure)
                            }
                        }
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    it.rawSlot == openStructure.drop.size -> {
                        player.openMenu<Basic>("编辑开采结构 $id") {
                            rows(3)
                            onClose { close ->
                                close.inventory.filter { item -> Items.nonNull(item) }.forEach { item ->
                                    val itemId = Utils.itemId(item)
                                    if (itemId != null) {
                                        openProgress.structures.filter { structure -> structure.origin == openStructure.origin }
                                            .forEach { structure ->
                                                structure.drop.add(BlockDrop(itemId, item.amount, 0.0))
                                            }
                                    }
                                }
                                BlockMine.export()
                                Bukkit.getScheduler().runTaskLater(Sandalphon.plugin, Runnable {
                                    openEditDrop(player, openProgress, openStructure)
                                }, 1)
                            }
                        }
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                }
            }
            onClose {
                BlockMine.export()
            }
        }
    }

    companion object {

        val isAfter11300 by lazy { MinecraftVersion.majorLegacy >= 11300 }
    }
}