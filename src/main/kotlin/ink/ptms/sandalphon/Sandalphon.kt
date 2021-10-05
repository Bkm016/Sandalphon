package ink.ptms.sandalphon

import ink.ptms.sandalphon.module.Database
import ink.ptms.zaphkiel.module.Vars
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.SecuredFile

object Sandalphon : Plugin() {

    @Config
    lateinit var conf: SecuredFile
        private set

    val database by lazy {
        Database()
    }

    val playerVars = HashMap<String, Vars>()

    @SubscribeEvent
    internal fun e(e: PlayerJoinEvent) {
        submit { playerVars[e.player.name] = Vars(e.player.name, database[e.player.name].toMutableMap()) }
    }

    @SubscribeEvent
    internal fun e(e: PlayerQuitEvent) {
        playerVars.remove(e.player.name)
    }
}