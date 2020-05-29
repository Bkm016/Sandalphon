package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.module.command.base.*
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.lite.Numbers
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

/**
 * @Author sky
 * @Since 2020-05-20 17:51
 */
@BaseCommand(name = "spawner", permission = "admin")
class SpawnerCommand : BaseMainCommand(), Helper {

    @SubCommand(priority = 0.0, description = "新建刷怪箱", arguments = ["生物"], type = CommandType.PLAYER)
    fun create(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            sender.error("该功能依赖 MythicMobs 插件.")
            return
        }
        val mob = MythicMobs.inst().mobManager.getMythicMob(args[0])
        if (mob == null) {
            sender.error("无效的生物.")
            return
        }
        val block = (sender as Player).getTargetBlockExact(10, FluidCollisionMode.NEVER)
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val spawnerData = Spawner.getSpawner(block)
        if (spawnerData != null) {
            block.display()
            sender.error("该方块已存在刷怪箱.")
            return
        }
        block.display()
        sender.info("刷怪箱已创建.")
        Spawner.spawners.add(SpawnerData(block.location, mob).run {
            this.openEdit(sender)
            this
        })
        Spawner.export()
    }

    @SubCommand(priority = 0.1, description = "移除刷怪箱", type = CommandType.PLAYER)
    fun remove(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            sender.error("该功能依赖 MythicMobs 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact(10, FluidCollisionMode.NEVER)
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val spawnerData = Spawner.getSpawner(block)
        if (spawnerData == null) {
            block.display()
            sender.error("该方块不存在刷怪箱.")
            return
        }
        block.display()
        sender.info("刷怪箱已移除.")
        spawnerData.cancel()
        Spawner.spawners.remove(spawnerData)
        Spawner.delete(Utils.fromLocation(spawnerData.block))
        Spawner.export()
    }

    @SubCommand(priority = 0.2, description = "编辑刷怪箱", type = CommandType.PLAYER)
    fun edit(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            sender.error("该功能依赖 MythicMobs 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact(10, FluidCollisionMode.NEVER)
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val spawnerData = Spawner.getSpawner(block)
        if (spawnerData == null) {
            block.display()
            sender.error("该方块不存在刷怪箱.")
            return
        }
        spawnerData.openEdit(sender)
        sender.info("正在编辑刷怪箱.")
    }

    @SubCommand(priority = 0.4, description = "复制刷怪箱", type = CommandType.PLAYER)
    fun copy(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            sender.error("该功能依赖 MythicMobs 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact(10, FluidCollisionMode.NEVER)
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val spawnerData = Spawner.getSpawner(block)
        if (spawnerData == null) {
            block.display()
            sender.error("该方块不存在脚本.")
            return
        }
        block.display()
        sender.info("使用§f链接魔杖§7右键方块创建拷贝, 左键方块移除拷贝.")
        CronusUtils.addItem(sender, ItemBuilder(Material.BLAZE_ROD).name("§f§f§f拷贝魔杖").lore("§7Spawner", "§7${Utils.fromLocation(spawnerData.block)}").shiny().build())
    }

    @SubCommand(priority = 0.41, description = "附近刷怪箱", type = CommandType.PLAYER)
    fun near(sender: CommandSender, args: Array<String>) {
        sender.info("附近刷怪箱:")
        Spawner.spawners.forEach {
            if (it.block.world?.name == (sender as Player).world.name && it.block.distance(sender.location) < 50) {
                it.block.block.display()
                it.copy.forEach { link -> link.block.display() }
                sender.info("§8 - §f${it.mob.internalName} §7(${Numbers.format(it.block.distance(sender.location))}m)")
            }
        }
    }

    @SubCommand(priority = 0.5, description = "重载刷怪箱")
    fun import(sender: CommandSender, args: Array<String>) {
        Spawner.data.load(File(Sandalphon.getPlugin().dataFolder, "module/spawner.yml"))
        Spawner.import()
        sender.info("操作成功.")
    }
}