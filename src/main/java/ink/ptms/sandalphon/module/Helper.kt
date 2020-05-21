package ink.ptms.sandalphon.module

import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.module.locale.TLocale
import io.izzel.taboolib.util.lite.cooldown.Cooldown
import org.bukkit.Effect
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

interface Helper {

    fun CommandSender.info(value: String) {
        this.sendMessage("§c[Sandalphon] §7${value.replace("&", "§")}")
        if (this is Player && !Global.cd.isCooldown(this.name)) {
            this.playSound(this.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
        }
    }

    fun CommandSender.error(value: String) {
        this.sendMessage("§c[Sandalphon] §7${value.replace("&", "§")}")
        if (this is Player && !Global.cd.isCooldown(this.name)) {
            this.playSound(this.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    fun Block.display() {
        world.playEffect(location, Effect.STEP_SOUND, blockData.material)
    }

    fun String.unColored(): String {
        return TLocale.Translate.setUncolored(this)
    }

    object Global {

        @TInject
        val cd = Cooldown("command.sound", 50)
    }
}