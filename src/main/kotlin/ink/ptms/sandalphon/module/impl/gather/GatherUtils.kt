package ink.ptms.sandalphon.module.impl.gather

import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun CommandSender.error(vararg messages: String) {
    messages.forEach { sendMessage("§c[Sandalphon] §7${it.replace("&", "§")}") }
    if (this is Player) {
        playSound(location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }
}

fun CommandSender.success(vararg messages: String) {
    messages.forEach { sendMessage("§c[Sandalphon] §7${it.replace("&", "§")}") }
    if (this is Player) {
        playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
    }
}

fun CommandSender.info(vararg messages: String) {
    messages.forEach { sendMessage("§c[Sandalphon] §7${it.replace("&", "§")}") }
    if (this is Player) {
        playSound(location, Sound.UI_BUTTON_CLICK, 1f, 2f)
    }
}

fun createLoad(max: Double, now: Double, step: Int = 20): String {
    if (max == 0.0) return "§7${"▱".repeat(step)}"
    val progress = 1.0 - (now / max)
    val filledBars = (progress * step).toInt()
    val emptyBars = step - filledBars
    return "§a${"▰".repeat(filledBars)}§7${"▱".repeat(emptyBars)}"
}
