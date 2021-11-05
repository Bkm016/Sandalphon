package ink.ptms.sandalphon.util

import com.google.common.base.Enums
import com.google.gson.*
import ink.ptms.zaphkiel.ZaphkielAPI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector
import taboolib.library.xseries.parseToMaterial

object Utils {

    val serializer = GsonBuilder().excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(
            Vector::class.java,
            JsonSerializer<Vector> { a, _, _ -> JsonPrimitive("${a.x},${a.y},${a.z}") })
        .registerTypeAdapter(
            Vector::class.java,
            JsonDeserializer { a, _, _ ->
                a.asString.split(",").run { Vector(this[0].asDouble(), this[1].asDouble(), this[2].asDouble()) }
            })
        .registerTypeAdapter(Material::class.java, JsonSerializer<Material> { a, _, _ -> JsonPrimitive(a.name) })
        .registerTypeAdapter(Material::class.java, JsonDeserializer { a, _, _ -> a.asString.parseToMaterial() })
        .registerTypeAdapter(
            Location::class.java,
            JsonSerializer<Location> { a, _, _ -> JsonPrimitive(fromLocation(a)) })
        .registerTypeAdapter(Location::class.java, JsonDeserializer { a, _, _ -> toLocation(a.asString) })
        .registerTypeAdapter(BlockFace::class.java, JsonSerializer<BlockFace> { a, _, _ -> JsonPrimitive(a.name) })
        .registerTypeAdapter(
            BlockFace::class.java,
            JsonDeserializer { a, _, _ -> Enums.getIfPresent(BlockFace::class.java, a.asString).or(BlockFace.SELF) })
        .create()!!

    fun itemId(itemStack: ItemStack): String? {
        val itemStream = ZaphkielAPI.read(itemStack)
        if (itemStream.isExtension()) {
            return itemStream.getZaphkielName()
        }
        return null
    }

    fun format(json: JsonElement): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(json)
    }

    fun fromLocation(location: Location): String {
        return "${location.world?.name},${location.x},${location.y},${location.z}"
    }

    fun toLocation(source: String): Location {
        return source.split(",").run {
            Location(
                Bukkit.getWorld(get(0)),
                getOrElse(1) { "0" }.asDouble(),
                getOrElse(2) { "0" }.asDouble(),
                getOrElse(3) { "0" }.asDouble()
            )
        }
    }

    fun String.asDouble(): Double {
        return NumberConversions.toDouble(this)
    }
}