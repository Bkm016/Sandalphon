package ink.ptms.sandalphon.module.impl.scriptblock

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockData
import ink.ptms.sandalphon.module.impl.scriptblock.data.openEdit
import ink.ptms.sandalphon.util.ItemBuilder
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.getDataFolder
import taboolib.common5.Coerce
import taboolib.expansion.createHelper
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.giveItem
import java.io.File

/**
 * @author sky
 * @since 2020-05-20 17:51
 */
@CommandHeader(name = "scriptblock", aliases = ["sb"], permission = "admin")
object ScriptBlockCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommand {
        execute<Player> { sender, _, _ ->
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val blockData = ScriptBlock.getBlock(block)
            if (blockData != null) {
                block.display()
                sender.error("该方块已存在脚本.")
                return@execute
            }
            block.display()
            sender.info("脚本方块已创建.")
            ScriptBlock.blocks.add(BlockData(block.location).run {
                openEdit(sender)
                this
            })
            ScriptBlock.export()
        }
    }

    @CommandBody
    val remove = subCommand {
        execute<Player> { sender, _, _ ->
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val blockData = ScriptBlock.getBlock(block)
            if (blockData == null) {
                block.display()
                sender.error("该方块不存在脚本.")
                return@execute
            }
            block.display()
            sender.info("脚本方块已移除.")
            ScriptBlock.blocks.remove(blockData)
            ScriptBlock.delete(Utils.fromLocation(blockData.block))
            ScriptBlock.export()
        }
    }

    @CommandBody
    val edit = subCommand {
        execute<Player> { sender, _, _ ->
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val blockData = ScriptBlock.getBlock(block)
            if (blockData == null) {
                block.display()
                sender.error("该方块不存在脚本.")
                return@execute
            }
            blockData.openEdit(sender)
            sender.info("正在编辑脚本.")
        }
    }

    @CommandBody
    val link = subCommand {
        execute<Player> { sender, _, _ ->
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val blockData = ScriptBlock.getBlock(block)
            if (blockData == null) {
                block.display()
                sender.error("该方块不存在脚本.")
                return@execute
            }
            block.display()
            sender.info("使用§f链接魔杖§7右键方块创建连接, 左键方块移除连接.")
            sender.giveItem(ItemBuilder(XMaterial.BLAZE_ROD)
                .name("§f§f§f链接魔杖")
                .lore("§7ScriptBlock", "§7${Utils.fromLocation(blockData.block)}")
                .shiny()
                .build()
            )
        }
    }

    @CommandBody
    val near = subCommand {
        execute<Player> { sender, _, _ ->
            sender.info("附近脚本:")
            ScriptBlock.blocks.forEach {
                if (it.block.world?.name == sender.world.name && it.block.distance(sender.location) < 50) {
                    it.block.block.display()
                    it.link.forEach { link -> link.block.display() }
                    sender.info("§8 - §f${Utils.fromLocation(it.block)} §7(${Coerce.format(it.block.distance(sender.location))}m)")
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<CommandSender> { sender, _, _ ->
            ScriptBlock.data.load(File(getDataFolder(), "module/scriptblock.yml"))
            ScriptBlock.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<CommandSender> { sender, _, _ ->
            ScriptBlock.export()
            sender.info("操作成功.")
        }
    }
}