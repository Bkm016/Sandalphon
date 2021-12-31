package ink.ptms.sandalphon

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.io.newFile
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.getDataFolder
import taboolib.expansion.releaseDataContainer
import taboolib.expansion.setupDataContainer
import taboolib.expansion.setupPlayerDatabase
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Kether

object Sandalphon : Plugin() {

    @Config
    lateinit var conf: Configuration
        private set

    override fun onEnable() {
        Kether.isAllowToleranceParser = true
        try {
            if (conf.getBoolean("Database.enable")) {
                setupPlayerDatabase(conf.getConfigurationSection("Database")!!)
            } else {
                setupPlayerDatabase(newFile(getDataFolder(), "data.db"))
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            disablePlugin()
            return
        }
    }

    @SubscribeEvent
    internal fun e(e: PlayerJoinEvent) {
        e.player.setupDataContainer()
    }

    @SubscribeEvent
    internal fun e(e: PlayerQuitEvent) {
        e.player.releaseDataContainer()
    }
}