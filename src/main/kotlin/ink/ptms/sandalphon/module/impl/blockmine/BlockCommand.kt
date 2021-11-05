package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.blockmine.data.openEdit
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common5.Coerce
import taboolib.expansion.createHelper
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import taboolib.platform.util.giveItem

/**
 * @author sky
 * @since 2020-06-01 17:49
 */
@CommandHeader(name = "blockmine", aliases = ["mine"], permission = "admin")
object BlockCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommand {
        dynamic(commit = "id") {
            suggestion<Player>(uncheck = true) { _, _ -> BlockMine.blocks.map { it.id } }
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                    sender.error("该功能依赖 Zaphkiel 插件.")
                    return@execute
                }
                val blockData = BlockMine.getBlock(argument)
                if (blockData != null) {
                    sender.error("该开采结构已存在.")
                    return@execute
                }
                sender.info("开采结构已创建.")
                BlockMine.blocks.add(BlockData(argument).also { it.openEdit(sender) })
                BlockMine.export()
            }
        }
    }

    @CommandBody
    val remove = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> BlockMine.blocks.map { it.id } }
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                    sender.error("该功能依赖 Zaphkiel 插件.")
                    return@execute
                }
                val blockData = BlockMine.getBlock(argument)
                if (blockData == null) {
                    sender.error("该开采结构不存在.")
                    return@execute
                }
                sender.info("开采结构已移除.")
                BlockMine.blocks.remove(blockData)
                BlockMine.delete(blockData.id)
                BlockMine.export()
            }
        }
    }

    @CommandBody
    val edit = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> BlockMine.blocks.map { it.id } }
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                    sender.error("该功能依赖 Zaphkiel 插件.")
                    return@execute
                }
                val blockData = BlockMine.getBlock(argument)
                if (blockData == null) {
                    sender.error("该开采结构不存在.")
                    return@execute
                }
                blockData.openEdit(sender)
                sender.info("正在编辑开采结构.")
            }
        }
    }

    @CommandBody
    val into = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> BlockMine.blocks.map { it.id } }
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                    sender.error("该功能依赖 Zaphkiel 插件.")
                    return@execute
                }
                val blockData = BlockMine.getBlock(argument)
                if (blockData == null) {
                    sender.error("该开采结构不存在.")
                    return@execute
                }
                sender.info("使用§f场景魔杖§7左键方块创建实例, 右键方块移除实例.")
                sender.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                    name = "§f§f§f场景魔杖"
                    lore += "§7BlockMine"
                    lore += "§7${blockData.id}"
                    shiny()
                })
            }
        }
    }

    @CommandBody
    val debug = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> BlockMine.blocks.map { it.id } }
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                    sender.error("该功能依赖 Zaphkiel 插件.")
                    return@execute
                }
                val blockData = BlockMine.getBlock(argument)
                if (blockData == null) {
                    sender.error("该开采结构不存在.")
                    return@execute
                }
                sender.info("使用§f调试魔杖§7左键方块重建实例, 右键方块切换阶段.")
                sender.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                    name = "§f§f§f调试魔杖"
                    lore += "§7BlockMine"
                    lore += "§7${blockData.id}"
                    shiny()
                })
            }
        }
    }

    @CommandBody
    val near = subCommand {
        execute<Player> { sender, _, _ ->
            sender.info("附近开采结构:")
            BlockMine.blocks.forEach {
                it.blocks.forEach { state ->
                    if (state.location.world?.name == sender.world.name && state.location.distance(sender.location) < 50) {
                        sender.info("§8 - §f${it.id} §7(${Coerce.format(state.location.distance(sender.location))}m)")
                    }
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<CommandSender> { sender, _, _ ->
            BlockMine.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<CommandSender> { sender, _, _ ->
            BlockMine.export()
            sender.info("操作成功.")
        }
    }
}