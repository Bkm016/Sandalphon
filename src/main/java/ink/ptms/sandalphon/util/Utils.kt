package ink.ptms.sandalphon.util

import com.google.common.base.Enums
import io.izzel.taboolib.internal.gson.*
import io.izzel.taboolib.util.item.Items
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector

object Utils {

    val serializer = GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Vector::class.java, JsonSerializer<Vector> { a, _, _ -> JsonPrimitive("${a.x},${a.y},${a.z}") })
            .registerTypeAdapter(Vector::class.java, JsonDeserializer<Vector> { a, _, _ -> a.asString.split(",").run { Vector(this[0].asDouble(),this[1].asDouble(), this[2].asDouble()) } })
            .registerTypeAdapter(Material::class.java, JsonSerializer<Material> { a, _, _ -> JsonPrimitive(a.name) })
            .registerTypeAdapter(Material::class.java, JsonDeserializer<Material> { a, _, _ -> Items.asMaterial(a.asString) })
            .registerTypeAdapter(Location::class.java, JsonSerializer<Location> { a, _, _ -> JsonPrimitive(fromLocation(a)) })
            .registerTypeAdapter(Location::class.java, JsonDeserializer<Location> { a, _, _ -> toLocation(a.asString) })
            .registerTypeAdapter(BlockFace::class.java, JsonSerializer<BlockFace> { a, _, _ -> JsonPrimitive(a.name) })
            .registerTypeAdapter(BlockFace::class.java, JsonDeserializer<BlockFace> { a, _, _ -> Enums.getIfPresent(BlockFace::class.java, a.asString).or(BlockFace.SELF) })
            .create()

    fun format(json: JsonElement): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(json)
    }

    fun fromLocation(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z}"
    }

    fun toLocation(source: String): Location {
        return source.split(",").run {
            Location(Bukkit.getWorld(get(0)), getOrElse(1) { "0" }.asDouble(), getOrElse(2) { "0" }.asDouble(), getOrElse(3) { "0" }.asDouble())
        }
    }

    private fun String.asDouble(): Double {
        return NumberConversions.toDouble(this)
    }
}