package ink.ptms.sandalphon.module.impl.recipes

import ink.ptms.sandalphon.module.Helper
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.onlinePlayers
import taboolib.expansion.createHelper
import taboolib.module.nms.MinecraftVersion

/**
 * Sandalphon
 * ink.ptms.sandalphon.module.impl.recipes.RecipesCommand
 *
 * @author sky
 * @since 2021/3/15 6:42 上午
 */
@CommandHeader(name = "recipes", permission = "admin")
object RecipesCommand : Helper {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val open = subCommand {
        execute<Player> { sender, _, _ ->
            if (MinecraftVersion.majorLegacy >= 11300) {
                sender.openRecipes()
            } else {
                sender.info("Sandalphon Recipes 需要 Minecraft 1.13 或以上版本。")
            }
        }
    }

    @CommandBody
    val discover = subCommand {
        dynamic {
            suggestion<CommandSender> { _, _ -> onlinePlayers().map { it.name } }
            dynamic {
                suggestion<CommandSender> { _, _ -> Recipes.recipeMap.values.flatMap { it.recipes.keys.map { key -> key.key } } }
                execute<CommandSender> { sender, context, argument ->
                    Bukkit.getPlayerExact(context.argument(-1))!!.discoverRecipe(NamespacedKey.minecraft(argument))
                    sender.info("操作成功.")
                }
            }
        }
    }

    @CommandBody
    val undiscover = subCommand {
        dynamic {
            suggestion<CommandSender> { _, _ -> onlinePlayers().map { it.name } }
            dynamic {
                suggestion<CommandSender> { _, _ -> Recipes.recipeMap.values.flatMap { it.recipes.keys.map { key -> key.key } } }
                execute<CommandSender> { sender, context, argument ->
                    Bukkit.getPlayerExact(context.argument(-1))!!.undiscoverRecipe(NamespacedKey.minecraft(argument))
                    sender.info("操作成功.")
                }
            }
        }
    }

    @CommandBody
    val import = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Recipes.import()
            sender.info("操作成功.")
        }
    }

    @CommandBody
    val export = subCommand {
        execute<CommandSender> { sender, _, _ ->
            Recipes.export()
            sender.info("操作成功.")
        }
    }
}