package ink.ptms.sandalphon.module

import org.bukkit.Effect
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.Baffle
import taboolib.module.nms.MinecraftVersion
import java.util.concurrent.TimeUnit

interface Helper {

    fun Player.getTargetBlockExact(): Block? {
        return if (MinecraftVersion.majorLegacy >= 11300) {
            getTargetBlockExact(10, FluidCollisionMode.NEVER)
        } else {
            getTargetBlock(setOf(Material.AIR), 10)
        }
    }

    fun CommandSender.info(value: String) {
        sendMessage("§c[Sandalphon] §7${value.replace("&", "§")}")
        if (this is Player && Global.cd.hasNext(this.name)) {
            playSound(this.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
        }
    }

    fun CommandSender.error(value: String) {
        sendMessage("§c[Sandalphon] §7${value.replace("&", "§")}")
        if (this is Player && Global.cd.hasNext(this.name)) {
            playSound(this.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    fun Block.display() {
        world.playEffect(location, Effect.STEP_SOUND, type)
    }

    object Global {

        val cd = Baffle.of(50, TimeUnit.MILLISECONDS)

        @SubscribeEvent
        fun e(e: PlayerQuitEvent) {
            cd.reset(e.player.name)
        }
    }
}