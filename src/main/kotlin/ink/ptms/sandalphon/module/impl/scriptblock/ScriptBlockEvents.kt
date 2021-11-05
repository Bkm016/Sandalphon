package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.util.Utils
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.BookMeta
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.chat.uncolored
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.hasLore
import taboolib.platform.util.hasName

/**
 * @author sky
 * @since 2020-05-21 13:33
 */
object ScriptBlockEvents : Helper {

    @SubscribeEvent
    fun e(e: BlockBreakEvent) {
        if (e.player.isOp && e.player.inventory.itemInMainHand.hasName("链接魔杖") && e.player.inventory.itemInMainHand.hasLore("ScriptBlock")) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].uncolored())
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

    @SubscribeEvent
    fun e(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) {
            return
        }
        if (e.player.isOp
            && e.action == Action.RIGHT_CLICK_BLOCK
            && e.player.inventory.itemInMainHand.hasName("链接魔杖")
            && e.player.inventory.itemInMainHand.hasLore("ScriptBlock")
        ) {
            e.isCancelled = true
            val location = Utils.toLocation(e.player.inventory.itemInMainHand.itemMeta!!.lore!![1].uncolored())
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
                if (blockType == BlockType.INTERACT) {
                    check(e.player).thenAccept { cond ->
                        if (cond) {
                            eval(e.player)
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerMoveEvent) {
        if (e.to != null && e.from.block != e.to!!.block) {
            ScriptBlock.getBlock(e.to!!.block.getRelative(BlockFace.DOWN))?.run {
                if (blockType == BlockType.WALK) {
                    check(e.player).thenAccept { cond ->
                        if (cond) {
                            eval(e.player)
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun e(e: ProjectileHitEvent) {
        if (e.hitBlock != null && e.entity.shooter is Player) {
            ScriptBlock.getBlock(e.hitBlock!!)?.run {
                if (blockType == BlockType.PROJECTILE) {
                    check(e.entity.shooter as Player).thenAccept { cond ->
                        if (cond) {
                            eval(e.entity.shooter as Player)
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerEditBookEvent) {
        if (!e.player.isOp) {
            return
        }
        if (e.previousBookMeta.displayName.contains("编辑动作") && e.previousBookMeta.lore!![0].uncolored() == "ScriptBlock") {
            val blockData = ScriptBlock.getBlock(Utils.toLocation(e.previousBookMeta.lore!![1].uncolored()).block)
            if (blockData == null) {
                e.player.error("该脚本已失效. (${e.newBookMeta.author})")
            } else {
                blockData.action.clear()
                if (e.newBookMeta.pages[0].uncolored() != "clear") {
                    blockData.action.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                if (MinecraftVersion.majorLegacy < 11300 && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        } else if (e.previousBookMeta.displayName.contains("编辑条件") && e.previousBookMeta.lore!![0].uncolored() == "ScriptBlock") {
            val blockData = ScriptBlock.getBlock(Utils.toLocation(e.previousBookMeta.lore!![1].uncolored()).block)
            if (blockData == null) {
                e.player.error("该脚本已失效. (${e.newBookMeta.author})")
            } else {
                blockData.condition.clear()
                if (e.newBookMeta.pages[0].uncolored() != "clear") {
                    blockData.condition.addAll(e.newBookMeta.pages.flatMap { it.replace("§0", "").split("\n") })
                }
                if (MinecraftVersion.majorLegacy < 11300 && e.player.itemInHand.itemMeta is BookMeta) {
                    e.player.setItemInHand(null)
                }
            }
            e.isSigning = false
        }
    }
}