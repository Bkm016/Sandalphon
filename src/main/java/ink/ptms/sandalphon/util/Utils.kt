package ink.ptms.sandalphon.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.NumberConversions

object Utils {

    fun String.asDouble(): Double {
        return NumberConversions.toDouble(this)
    }

    fun fromLocation(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z}"
    }

    fun toLocation(source: String): Location {
        return source.split(",").run {
            Location(Bukkit.getWorld(get(0)), getOrElse(1) { "0" }.asDouble(), getOrElse(2) { "0" }.asDouble(), getOrElse(3) { "0" }.asDouble())
        }
    }
}