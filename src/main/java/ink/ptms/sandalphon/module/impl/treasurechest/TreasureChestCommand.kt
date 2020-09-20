package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.command.base.*
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Numbers
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
@BaseCommand(name = "treasurechest", aliases = ["tchest"], permission = "admin")
class TreasureChestCommand : BaseMainCommand(), Helper {

    @SubCommand(priority = 0.0, description = "新建宝藏", type = CommandType.PLAYER)
    fun create(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            sender.error("该功能依赖 Zaphkiel 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact()
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        if (TreasureChest.getChest(block) != null) {
            block.display()
            sender.error("该方块已存在宝藏.")
            return
        }
        block.display()
        sender.info("宝藏已创建.")
        TreasureChest.chests.add(ChestData(block.location).run {
            this.openEdit(sender)
            this
        })
        TreasureChest.export()
    }

    @SubCommand(priority = 0.1, description = "移除宝藏", type = CommandType.PLAYER)
    fun remove(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            sender.error("该功能依赖 Zaphkiel 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact()
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val chestData = TreasureChest.getChest(block)
        if (chestData == null) {
            block.display()
            sender.error("该方块不存在宝藏.")
            return
        }
        block.display()
        sender.info("宝藏已移除.")
        TreasureChest.chests.remove(chestData)
        TreasureChest.delete(Utils.fromLocation(chestData.block))
        TreasureChest.export()
    }

    @SubCommand(priority = 0.2, description = "编辑宝藏", type = CommandType.PLAYER)
    fun edit(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            sender.error("该功能依赖 Zaphkiel 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact()
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val chestData = TreasureChest.getChest(block)
        if (chestData == null) {
            block.display()
            sender.error("该方块不存在宝藏.")
            return
        }
        chestData.openEdit(sender)
        sender.info("正在编辑宝藏.")
    }

    @SubCommand(priority = 0.21, description = "编辑宝藏内容", type = CommandType.PLAYER)
    fun peek(sender: CommandSender, args: Array<String>) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null && !Utils.asgardHook) {
            sender.error("该功能依赖 Zaphkiel 插件.")
            return
        }
        val block = (sender as Player).getTargetBlockExact()
        if (block == null || block.blockData.material == Material.AIR) {
            sender.error("无效的方块.")
            return
        }
        val chestData = TreasureChest.getChest(block)
        if (chestData == null) {
            block.display()
            sender.error("该方块不存在宝藏.")
            return
        }
        chestData.openEditContent(sender)
        sender.info("正在编辑宝藏内容.")
    }

    @SubCommand(priority = 0.41, description = "附近宝藏", type = CommandType.PLAYER)
    fun near(sender: CommandSender, args: Array<String>) {
        sender.info("附近宝藏:")
        TreasureChest.chests.forEach {
            if (it.block.world?.name == (sender as Player).world.name && it.block.distance(sender.location) < 50) {
                it.block.block.display()
                it.link.forEach { link -> link.block.display() }
                sender.info("§8 - §f${Utils.fromLocation(it.block)} §7(${Numbers.format(it.block.distance(sender.location))}m)")
            }
        }
    }

    @SubCommand(priority = 0.5, description = "重载宝藏")
    fun import(sender: CommandSender, args: Array<String>) {
        TreasureChest.data.load(File(Sandalphon.getPlugin().dataFolder, "module/treasurechest.yml"))
        TreasureChest.import()
        sender.info("操作成功.")
    }
}