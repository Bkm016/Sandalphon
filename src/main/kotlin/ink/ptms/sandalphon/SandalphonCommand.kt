package ink.ptms.sandalphon

import ink.ptms.sandalphon.module.impl.blockmine.BlockCommand
import ink.ptms.sandalphon.module.impl.holographic.HologramCommand
import ink.ptms.sandalphon.module.impl.recipes.RecipesCommand
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlockCommand
import ink.ptms.sandalphon.module.impl.spawner.SpawnerCommand
import ink.ptms.sandalphon.module.impl.treasurechest.TreasureChestCommand
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.expansion.createHelper

@CommandHeader(name = "sandalphon", aliases = ["sn"], permission = "*")
object SandalphonCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val blockmine = BlockCommand

    @CommandBody
    val hologram = HologramCommand

    @CommandBody
    val recipe = RecipesCommand

    @CommandBody
    val scriptblock = ScriptBlockCommand

    @CommandBody
    val spawner = SpawnerCommand

    @CommandBody
    val treasurechest = TreasureChestCommand
}