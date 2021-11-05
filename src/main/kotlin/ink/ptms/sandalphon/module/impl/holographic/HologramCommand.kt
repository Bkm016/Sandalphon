package ink.ptms.sandalphon.module.impl.holographic

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.holographic.data.HologramData
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.getDataFolder
import taboolib.common5.Coerce
import taboolib.expansion.createHelper
import java.io.File

/**
 * @author sky
 * @since 2020-05-20 17:51
 */
@CommandHeader(name = "hologram", aliases = ["hd"], permission = "admin")
object HologramCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommand {
        dynamic(commit = "id") {
            suggestion<Player>(uncheck = true) { _, _ -> Hologram.holograms.map { it.id } }
            execute<Player> { sender, _, argument ->
                val hologramData = Hologram.getHologram(argument)
                if (hologramData != null) {
                    sender.error("该全息已存在.")
                    return@execute
                }
                sender.info("全息已创建.")
                Hologram.holograms.add(HologramData(argument, sender.location.add(0.0, 1.0, 0.0)).run {
                    openEdit(sender)
                    this
                })
                Hologram.export()
            }
        }
    }

    @CommandBody
    val remove = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> Hologram.holograms.map { it.id } }
            execute<Player> { sender, _, argument ->
                val hologramData = Hologram.getHologram(argument)
                if (hologramData == null) {
                    sender.error("该全息不存在.")
                    return@execute
                }
                sender.info("全息已移除.")
                hologramData.cancel()
                Hologram.holograms.remove(hologramData)
                Hologram.delete(hologramData.id)
                Hologram.export()
            }
        }
    }

    @CommandBody
    val edit = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> Hologram.holograms.map { it.id } }
            execute<Player> { sender, _, argument ->
                val hologramData = Hologram.getHologram(argument)
                if (hologramData == null) {
                    sender.error("该全息不存在.")
                    return@execute
                }
                hologramData.openEdit(sender)
                sender.info("正在编辑全息.")
            }
        }
    }

    @CommandBody
    val move = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> Hologram.holograms.map { it.id } }
            execute<Player> { sender, _, argument ->
                val hologramData = Hologram.getHologram(argument)
                if (hologramData == null) {
                    sender.error("该全息不存在.")
                    return@execute
                }
                hologramData.location = sender.location.add(0.0, 1.0, 0.0)
                hologramData.init()
                Hologram.export()
                sender.info("正在移动全息.")
            }
        }
    }

    @CommandBody
    val tp = subCommand {
        dynamic(commit = "id") {
            suggestion<Player> { _, _ -> Hologram.holograms.map { it.id } }
            execute<Player> { sender, _, argument ->
                val hologramData = Hologram.getHologram(argument)
                if (hologramData == null) {
                    sender.error("该全息不存在.")
                    return@execute
                }
                sender.teleport(hologramData.location)
                sender.info("已传送至全息.")
            }
        }
    }

    @CommandBody
    val near = subCommand {
        execute<Player> { sender, _, _ ->
            sender.info("附近全息:")
            Hologram.holograms.forEach {
                if (it.location.world?.name == sender.world.name && it.location.distance(sender.location) < 50) {
                    sender.info("§8 - §f${it.id} §7(${Coerce.format(it.location.distance(sender.location))}m)")
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Hologram.data.load(File(getDataFolder(), "module/hologram.yml"))
            Hologram.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Hologram.export()
            sender.info("操作成功.")
        }
    }
}