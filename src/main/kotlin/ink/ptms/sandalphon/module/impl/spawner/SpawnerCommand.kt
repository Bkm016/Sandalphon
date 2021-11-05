package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.module.impl.spawner.data.openEdit
import ink.ptms.sandalphon.util.ItemBuilder
import ink.ptms.sandalphon.util.Utils
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
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
@CommandHeader(name = "spawner", aliases = ["ss"], permission = "admin")
object SpawnerCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommand {
        dynamic(commit = "mob") {
            execute<Player> { sender, _, argument ->
                if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                    sender.error("该功能依赖 MythicMobs 插件.")
                    return@execute
                }
                val mob = MythicMobs.inst().mobManager.getMythicMob(argument)
                if (mob == null) {
                    sender.error("无效的生物.")
                    return@execute
                }
                val block = sender.getTargetBlockExact()
                if (block == null || block.type == Material.AIR) {
                    sender.error("无效的方块.")
                    return@execute
                }
                val spawnerData = Spawner.getSpawner(block)
                if (spawnerData != null) {
                    block.display()
                    sender.error("该方块已存在刷怪箱.")
                    return@execute
                }
                block.display()
                sender.info("刷怪箱已创建.")
                Spawner.spawners.add(SpawnerData(block.location, mob).run {
                    this.openEdit(sender)
                    this
                })
                Spawner.export()
            }
        }
    }

    @CommandBody
    val remove = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                sender.error("该功能依赖 MythicMobs 插件.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val spawnerData = Spawner.getSpawner(block)
            if (spawnerData == null) {
                block.display()
                sender.error("该方块不存在刷怪箱.")
                return@execute
            }
            block.display()
            sender.info("刷怪箱已移除.")
            spawnerData.cancel()
            Spawner.spawners.remove(spawnerData)
            Spawner.delete(Utils.fromLocation(spawnerData.block))
            Spawner.export()
        }
    }

    @CommandBody
    val edit = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                sender.error("该功能依赖 MythicMobs 插件.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val spawnerData = Spawner.getSpawner(block)
            if (spawnerData == null) {
                block.display()
                sender.error("该方块不存在刷怪箱.")
                return@execute
            }
            spawnerData.openEdit(sender)
            sender.info("正在编辑刷怪箱.")
        }
    }

    @CommandBody
    val copy = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                sender.error("该功能依赖 MythicMobs 插件.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val spawnerData = Spawner.getSpawner(block)
            if (spawnerData == null) {
                block.display()
                sender.error("该方块不存在脚本.")
                return@execute
            }
            block.display()
            sender.info("使用§f拷贝魔杖§7右键方块创建拷贝, 左键方块移除拷贝.")
            sender.giveItem(ItemBuilder(XMaterial.BLAZE_ROD).name("§f§f§f拷贝魔杖").lore("§7Spawner", "§7${Utils.fromLocation(spawnerData.block)}").shiny().build())
        }
    }

    @CommandBody
    val near = subCommand {
        execute<Player> { sender, _, _ ->
            sender.info("附近刷怪箱:")
            Spawner.spawners.forEach {
                if (it.block.world?.name == sender.world.name && it.block.distance(sender.location) < 50) {
                    it.block.block.display()
                    it.copy.forEach { link -> link.block.display() }
                    sender.info("§8 - §f${it.mob.internalName} §7(${Coerce.format(it.block.distance(sender.location))}m)")
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Spawner.data.load(File(getDataFolder(), "module/spawner.yml"))
            Spawner.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Spawner.export()
            sender.info("操作成功.")
        }
    }
}