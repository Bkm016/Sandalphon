package ink.ptms.sandalphon.module.impl.recipes

import ink.ptms.sandalphon.module.Helper
import io.izzel.taboolib.Version
import io.izzel.taboolib.module.command.base.BaseCommand
import io.izzel.taboolib.module.command.base.BaseMainCommand
import io.izzel.taboolib.module.command.base.SubCommand
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipes.RecipesCommand
 *
 * @author sky
 * @since 2021/3/15 6:42 上午
 */
@BaseCommand(name = "recipes", permission = "admin")
class RecipesCommand : BaseMainCommand(), Helper {

    override fun onTabComplete(sender: CommandSender, command: String, argument: String): List<String>? {
        if ((command == "discover" || command == "undiscover") && argument == "配方") {
            return Recipes.recipeMap.values.flatMap {
                it.recipes.keys.map { key -> key.key }
            }
        }
        return null
    }

    @SubCommand(description = "打开界面", priority = 0.0)
    fun open(player: Player, args: Array<String>) {
        if (Version.isAfter(Version.v1_13)) {
            player.openRecipes()
        } else {
            player.info("Sandalphon Recipes 需要 Minecraft 1.13 或以上版本。")
        }
    }

    @SubCommand(description = "导入配置", priority = 1.0)
    fun import(sender: CommandSender, args: Array<String>) {
        Recipes.import()
        sender.info("操作成功.")
    }

    @SubCommand(description = "赋予配方认知", arguments = ["玩家", "配方"], priority = 2.0)
    fun discover(sender: CommandSender, args: Array<String>) {
        val playerExact = Bukkit.getPlayerExact(args[0])
        if (playerExact == null) {
            sender.error("玩家离线.")
            return
        }
        playerExact.discoverRecipe(NamespacedKey.minecraft(args[1]))
    }

    @SubCommand(description = "移除配方认知", arguments = ["玩家", "配方"], priority = 3.0)
    fun undiscover(sender: CommandSender, args: Array<String>) {
        val playerExact = Bukkit.getPlayerExact(args[0])
        if (playerExact == null) {
            sender.error("玩家离线.")
            return
        }
        playerExact.undiscoverRecipe(NamespacedKey.minecraft(args[1]))
    }
}