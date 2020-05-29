package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.module.locale.TLocale
import io.izzel.taboolib.util.item.Items
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * @Author sky
 * @Since 2020-05-21 13:33
 */
@TListener(depend = ["Cronus"])
class ScriptBlockEvents : Listener, Helper {

    @EventHandler
    fun e(e: BlockBreakEvent) {
        if (e.player.isOp && Items.hasName(e.player.inventory.itemInMainHand, "链接魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "ScriptBlock")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![0].unColored())
            val blockData = ScriptBlock.getBlock(location.block)
            if (blockData == null) {
                e.player.error("该方块不存在脚本.")
                return
            }
            if (e.block.location !in blockData.link) {
                e.player.error("该方块不被链接.")
                return
            }
            e.block.display()
            e.player.info("移除脚本链接.")
            blockData.block.block.display()
            blockData.link.remove(e.block.location)
            ScriptBlock.export()
        }
    }

    @EventHandler
    fun e(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        if (e.player.isOp && e.action == Action.RIGHT_CLICK_BLOCK && Items.hasName(e.player.inventory.itemInMainHand, "链接魔杖") && Items.hasLore(e.player.inventory.itemInMainHand, "ScriptBlock")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![0].unColored())
            val blockData = ScriptBlock.getBlock(location.block)
            if (blockData == null) {
                e.player.error("该方块不存在脚本.")
                return
            }
            if (e.clickedBlock!!.location in blockData.link) {
                e.player.error("该方块已被链接.")
                return
            }
            e.clickedBlock!!.display()
            e.player.info("创建脚本链接.")
            blockData.block.block.display()
            blockData.link.add(e.clickedBlock!!.location)
            ScriptBlock.export()
        } else if (e.action == Action.RIGHT_CLICK_BLOCK || e.action == Action.LEFT_CLICK_BLOCK) {
            ScriptBlock.getBlock(e.clickedBlock!!)?.run {
                if (this.blockType == BlockType.INTERACT && this.check(e.player)) {
                    this.eval(e.player)
                }
            }
        }
    }

    @EventHandler
    fun e(e: PlayerMoveEvent) {
        if (e.to != null && e.from.block != e.to!!.block) {
            ScriptBlock.getBlock(e.to!!.block.getRelative(BlockFace.DOWN))?.run {
                if (this.blockType == BlockType.WALK && this.check(e.player)) {
                    this.eval(e.player)
                }
            }
        }
    }

    @EventHandler
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑动作") && e.previousBookMeta.lore!![0].unColored() == "ScriptBlock") {
            val blockData = ScriptBlock.getBlock(Utils.toLocation(e.previousBookMeta.lore!![1].unColored()).block)
            if (blockData == null) {
                e.player.error("该脚本已失效. (${e.newBookMeta.author})")
            } else {
                blockData.blockAction.clear()
                if (e.newBookMeta.pages[0].unColored() != "clear") {
                    blockData.blockAction.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                blockData.init()
                blockData.openEdit(e.player)
            }
        } else if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].unColored() == "ScriptBlock") {
            val blockData = ScriptBlock.getBlock(Utils.toLocation(e.previousBookMeta.lore!![1].unColored()).block)
            if (blockData == null) {
                e.player.error("该脚本已失效. (${e.newBookMeta.author})")
            } else {
                blockData.blockCondition.clear()
                if (e.newBookMeta.pages[0].unColored() != "clear") {
                    blockData.blockCondition.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                blockData.init()
                blockData.openEdit(e.player)
            }
        }
    }
}