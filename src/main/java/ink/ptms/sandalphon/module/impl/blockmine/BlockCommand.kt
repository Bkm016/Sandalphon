package ink.ptms.sandalphon.module.impl.blockmine

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.blockmine.data.BlockData
import ink.ptms.sandalphon.module.impl.holographic.Hologram
import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import io.izzel.taboolib.module.command.base.*
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

/**
 * @Author sky
 * @Since 2020-06-01 17:49
 */
@BaseCommand(name = "blockmine", aliases = ["mine"], permission = "admin")
class BlockCommand : BaseMainCommand(), Helper {

    @SubCommand(priority = 0.0, description = "新建开采结构", arguments = ["序号"], type = CommandType.PLAYER)
    fun create(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
            sender.error("该功能依赖 Zaphkiel 插件.")
            return
        }
        val blockData = BlockMine.getBlock(args[0])
        if (blockData != null) {
            sender.error("该开采结构已存在.")
            return
        }
        sender.info("开采结构已创建.")
        BlockMine.blocks.add(BlockData(args[0]).run {
            this.openEdit(sender as Player)
            this
        })
        BlockMine.export()
    }

    @SubCommand(priority = 0.1, description = "移除开采结构", type = CommandType.PLAYER)
    val remove = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { BlockMine.blocks.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                sender.error("该功能依赖 Zaphkiel 插件.")
                return
            }
            val blockData = BlockMine.getBlock(args[0])
            if (blockData == null) {
                sender.error("该开采结构不存在.")
                return
            }
            sender.info("开采结构已移除.")
            BlockMine.blocks.remove(blockData)
            BlockMine.delete(blockData.id)
            BlockMine.export()
        }
    }

    @SubCommand(priority = 0.2, description = "修改开采结构", type = CommandType.PLAYER)
    val edit = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { BlockMine.blocks.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
                sender.error("该功能依赖 Zaphkiel 插件.")
                return
            }
            val blockData = BlockMine.getBlock(args[0])
            if (blockData == null) {
                sender.error("该开采结构不存在.")
                return
            }
            blockData.openEdit(sender as Player)
            sender.info("正在编辑开采结构.")
        }
    }

    @SubCommand(priority = 0.5, description = "附近开采结构", type = CommandType.PLAYER)
    fun near(sender: CommandSender, args: Array<String>) {
        sender.info("附近开采结构:")
        BlockMine.blocks.forEach {
            it.blocks.forEach { (k, _) ->
                if (k.world?.name == (sender as Player).world.name && k.distance(sender.location) < 50) {
                    sender.info("§8 - §f${it.id} §7(${Numbers.format(k.distance(sender.location))}m)")
                }
            }
        }
    }

    @SubCommand(priority = 0.6, description = "重载开采结构")
    fun import(sender: CommandSender, args: Array<String>) {
        BlockMine.data.load(File(Sandalphon.getPlugin().dataFolder, "module/blockmine.yml"))
        BlockMine.import()
        sender.info("操作成功.")
    }
}