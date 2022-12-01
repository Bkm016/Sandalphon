package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.module.impl.spawner.ai.FollowAi
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.module.impl.spawner.event.EntityToSpawnEvent
import ink.ptms.sandalphon.util.Utils
import ink.ptms.um.Mythic
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common5.clong
import taboolib.module.ai.addGoalAi
import taboolib.module.configuration.createLocal
import taboolib.platform.BukkitPlugin

@Suppress("SpellCheckingInspection")
object Spawner {

    val data by lazy { createLocal("module/spawner.yml") }
    val spawners = ArrayList<SpawnerData>()

    @Schedule(period = 20)
    fun tick() {
        spawners.forEach { it.tick() }
    }

    @Awake(LifeCycle.ACTIVE)
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            return
        }
        spawners.clear()
        data.getKeys(false).forEach { loc ->
            val mobType = Mythic.API.getMobType(data.getString("$loc.mob").toString())
            if (mobType != null) {
                spawners.add(SpawnerData(Utils.toLocation(loc.replace("__", ".")), mobType).also { s ->
                    val tv = data.getConfigurationSection("$loc.time")?.getValues(false) ?: emptyMap()
                    s.time.putAll(tv.map { Utils.toLocation(it.key.replace("__", ".")) to it.value.clong })
                    s.copy.addAll(data.getStringList("$loc.link").map { link -> Utils.toLocation(link) })
                    s.activationrange = data.getInt("$loc.activationrange")
                    s.leashrange = data.getInt("$loc.leashrange")
                    s.respawn = data.getInt("$loc.respawn")
                })
            }
        }
    }

    @Awake(LifeCycle.DISABLE)
    @Schedule(period = 20 * 60, async = true)
    fun export() {
        data.getKeys(false).forEach { data[it] = null }
        spawners.forEach { spawner ->
            val location = Utils.fromLocation(spawner.block).replace(".", "__")
            data["$location.time"] = spawner.time.map { Utils.fromLocation(it.key).replace(".", "__") to it.value }.toMap()
            data["$location.copy"] = spawner.copy.map { Utils.fromLocation(it) }
            data["$location.mob"] = spawner.mob.id
            data["$location.activationrange"] = spawner.activationrange
            data["$location.leashrange"] = spawner.leashrange
            data["$location.respawn"] = spawner.respawn
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun cancel() {
        spawners.forEach { it.cancel() }
    }

    fun delete(location: String) {
        data[location.replace(".", "__")] = null
    }

    fun getSpawner(block: Block): SpawnerData? {
        return spawners.firstOrNull { it.isSpawner(block) }
    }

    fun toSpawn(entity: LivingEntity): Boolean {
        val mythicMob = Mythic.API.getMob(entity)?.type ?: return false
        val spawnerData = spawners.firstOrNull { it.mob.id == mythicMob.id } ?: return false
        val pair = spawnerData.mobs.entries.firstOrNull { it.value.uniqueId == entity.uniqueId } ?: return false
        if (entity.location.world!!.name == pair.key.world!!.name) {
            entity.setMetadata("SPAWNER_BACKING", FixedMetadataValue(BukkitPlugin.getInstance(), true))
            entity.isInvulnerable = true
            entity.addGoalAi(FollowAi(entity, pair.key.clone().add(0.5, 1.5, 0.5), 1.5), 1)
            EntityToSpawnEvent.Start(entity, spawnerData).call()
        } else {
            entity.teleport(pair.key.clone().add(0.5, 1.5, 0.5))
        }
        return true
    }
}