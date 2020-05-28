package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
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
 * @Since 2020-05-20 17:51
 */
@BaseCommand(name = "hologram", aliases = ["hd"], permission = "admin")
class HologramCommand : BaseMainCommand(), Helper {

    @SubCommand(priority = 0.0, description = "新建全息", arguments = ["序号"], type = CommandType.PLAYER)
    fun create(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
            sender.error("该功能依赖 Cronus 插件.")
            return
        }
        val hologramData = Hologram.getHologram(args[0])
        if (hologramData != null) {
            sender.error("该全息已存在.")
            return
        }
        sender.info("全息已创建.")
        Hologram.holograms.add(HologramData(args[0], (sender as Player).location.add(0.0, 1.0, 0.0)).run {
            this.openEdit(sender)
            this
        })
        Hologram.export()
    }

    @SubCommand(priority = 0.1, description = "移除脚本", type = CommandType.PLAYER)
    val remove = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { Hologram.holograms.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
                sender.error("该功能依赖 Cronus 插件.")
                return
            }
            val hologramData = Hologram.getHologram(args[0])
            if (hologramData == null) {
                sender.error("该全息不存在.")
                return
            }
            sender.info("全息已移除.")
            hologramData.cancel()
            Hologram.holograms.remove(hologramData)
            Hologram.delete(hologramData.id)
            Hologram.export()
        }
    }

    @SubCommand(priority = 0.2, description = "修改全息", type = CommandType.PLAYER)
    val edit = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { Hologram.holograms.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
                sender.error("该功能依赖 Cronus 插件.")
                return
            }
            val hologramData = Hologram.getHologram(args[0])
            if (hologramData == null) {
                sender.error("该全息不存在.")
                return
            }
            hologramData.openEdit(sender as Player)
            sender.info("正在编辑全息.")
        }
    }

    @SubCommand(priority = 0.3, description = "移动全息", type = CommandType.PLAYER)
    val move = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { Hologram.holograms.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
                sender.error("该功能依赖 Cronus 插件.")
                return
            }
            val hologramData = Hologram.getHologram(args[0])
            if (hologramData == null) {
                sender.error("该全息不存在.")
                return
            }
            hologramData.location = (sender as Player).location.add(0.0, 1.0, 0.0)
            hologramData.init()
            Hologram.export()
            sender.info("正在移动全息.")
        }
    }

    @SubCommand(priority = 0.4, description = "传送全息", type = CommandType.PLAYER)
    val tp = object : BaseSubCommand() {

        override fun getArguments(): Array<Argument> {
            return arrayOf(Argument("序号") { Hologram.holograms.map { it.id }})
        }

        override fun onCommand(sender: CommandSender, p1: Command?, p2: String?, args: Array<out String>) {
            if (Bukkit.getPluginManager().getPlugin("Cronus") == null) {
                sender.error("该功能依赖 Cronus 插件.")
                return
            }
            val hologramData = Hologram.getHologram(args[0])
            if (hologramData == null) {
                sender.error("该全息不存在.")
                return
            }
            (sender as Player).teleport(hologramData.location)
            sender.info("已传送至全息.")
        }
    }

    @SubCommand(priority = 0.5, description = "附近全息", type = CommandType.PLAYER)
    fun near(sender: CommandSender, args: Array<String>) {
        sender.info("附近全息:")
        Hologram.holograms.forEach {
            if (it.location.world?.name == (sender as Player).world.name && it.location.distance(sender.location) < 50) {
                sender.info("§8 - §f${it.id} §7(${Numbers.format(it.location.distance(sender.location))}m)")
            }
        }
    }

    @SubCommand(priority = 0.6, description = "重载全息")
    fun import(sender: CommandSender, args: Array<String>) {
        Hologram.data.load(File(Sandalphon.getPlugin().dataFolder, "module/hologram.yml"))
        Hologram.import()
        sender.info("操作成功.")
    }
}