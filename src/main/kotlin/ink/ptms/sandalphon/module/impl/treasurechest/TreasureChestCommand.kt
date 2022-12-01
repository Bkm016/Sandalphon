package ink.ptms.sandalphon.module.impl.treasurechest

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.Helper
import ink.ptms.sandalphon.module.impl.treasurechest.data.ChestData
import ink.ptms.sandalphon.module.impl.treasurechest.data.openEdit
import ink.ptms.sandalphon.module.impl.treasurechest.data.openEditContent
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
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
@Suppress("DuplicatedCode")
@CommandHeader(name = "treasurechest", aliases = ["tchest"], permission = "admin")
object TreasureChestCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("Adyeshach") == null) {
                sender.error("该功能依赖 Adyeshach 插件.")
                return@execute
            }
            if (Sandalphon.itemAPI == null) {
                sender.error("缺少物品库兼容.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            if (TreasureChest.getChest(block) != null) {
                block.display()
                sender.error("该方块已存在宝藏.")
                return@execute
            }
            block.display()
            sender.info("宝藏已创建.")
            TreasureChest.chests.add(ChestData(block.location).also { it.openEdit(sender) })
            TreasureChest.export()
        }
    }

    @CommandBody
    val remove = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("Adyeshach") == null) {
                sender.error("该功能依赖 Adyeshach 插件.")
                return@execute
            }
            if (Sandalphon.itemAPI == null) {
                sender.error("缺少物品库兼容.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val chestData = TreasureChest.getChest(block)
            if (chestData == null) {
                block.display()
                sender.error("该方块不存在宝藏.")
                return@execute
            }
            block.display()
            sender.info("宝藏已移除.")
            TreasureChest.chests.remove(chestData)
            TreasureChest.delete(Utils.fromLocation(chestData.block))
            TreasureChest.export()
        }
    }

    @CommandBody
    val edit = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("Adyeshach") == null) {
                sender.error("该功能依赖 Adyeshach 插件.")
                return@execute
            }
            if (Sandalphon.itemAPI == null) {
                sender.error("缺少物品库兼容.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val chestData = TreasureChest.getChest(block)
            if (chestData == null) {
                block.display()
                sender.error("该方块不存在宝藏.")
                return@execute
            }
            chestData.openEdit(sender)
            sender.info("正在编辑宝藏.")
        }
    }

    @CommandBody
    val peek = subCommand {
        execute<Player> { sender, _, _ ->
            if (Bukkit.getPluginManager().getPlugin("Adyeshach") == null) {
                sender.error("该功能依赖 Adyeshach 插件.")
                return@execute
            }
            if (Sandalphon.itemAPI == null) {
                sender.error("缺少物品库兼容.")
                return@execute
            }
            val block = sender.getTargetBlockExact()
            if (block == null || block.type == Material.AIR) {
                sender.error("无效的方块.")
                return@execute
            }
            val chestData = TreasureChest.getChest(block)
            if (chestData == null) {
                block.display()
                sender.error("该方块不存在宝藏.")
                return@execute
            }
            chestData.openEditContent(sender)
            sender.info("正在编辑宝藏.")
        }
    }

    @CommandBody
    val near = subCommand {
        execute<Player> { sender, _, _ ->
            sender.info("附近宝藏:")
            TreasureChest.chests.forEach {
                if (it.block.world?.name == sender.world.name && it.block.distance(sender.location) < 50) {
                    it.block.block.display()
                    it.link?.block?.display()
                    sender.info("§8 - §f${Utils.fromLocation(it.block)} §7(${Coerce.format(it.block.distance(sender.location))}m)")
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<Player> { sender, _, _ ->
            TreasureChest.data.loadFromFile(File(getDataFolder(), "module/treasurechest.yml"))
            TreasureChest.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<Player> { sender, _, _ ->
            TreasureChest.export()
            sender.info("操作成功.")
        }
    }
}