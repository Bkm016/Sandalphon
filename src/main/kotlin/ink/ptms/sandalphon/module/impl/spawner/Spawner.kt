package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.module.impl.spawner.ai.FollowAi
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.module.impl.spawner.event.EntityToSpawnEvent
import ink.ptms.sandalphon.util.Utils
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.module.ai.addGoalAi
import taboolib.module.configuration.createLocal
import taboolib.platform.BukkitPlugin

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
            spawners.add(SpawnerData(Utils.toLocation(loc.replace("__", ".")), MythicMobs.inst().mobManager.getMythicMob(data.getString("$loc.mob"))).run {
                this.time.putAll(data.getConfigurationSection("$loc.time")?.getValues(false)
                    ?.map { Utils.toLocation(it.key.replace("__", ".")) to it.value as Long }?.toMap()
                    ?: emptyMap())
                this.copy.addAll(data.getStringList("$loc.copy").map { link -> Utils.toLocation(link) })
                this.activationrange = data.getInt("$loc.activationrange")
                this.leashrange = data.getInt("$loc.leashrange")
                this.respawn = data.getInt("$loc.respawn")
                this
            })
        }
    }

    @Awake(LifeCycle.DISABLE)
    @Schedule(period = 20 * 60, async = true)
    fun export() {
        data.getKeys(false).forEach { data.set(it, null) }
        spawners.forEach { spawner ->
            val location = Utils.fromLocation(spawner.block).replace(".", "__")
            data.set("$location.time", spawner.time.map { Utils.fromLocation(it.key).replace(".", "__") to it.value }.toMap())
            data.set("$location.copy", spawner.copy.map { Utils.fromLocation(it) })
            data.set("$location.mob", spawner.mob.internalName)
            data.set("$location.activationrange", spawner.activationrange)
            data.set("$location.leashrange", spawner.leashrange)
            data.set("$location.respawn", spawner.respawn)
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun cancel() {
        spawners.forEach { it.cancel() }
    }

    fun delete(location: String) {
        data.set(location.replace(".", "__"), null)
    }

    fun getSpawner(block: Block): SpawnerData? {
        return spawners.firstOrNull { it.isSpawner(block) }
    }

    fun toSpawn(entity: LivingEntity): Boolean {
        val mythicMob = MythicMobs.inst().mobManager.getMythicMobInstance(entity)?.type ?: return false
        val spawnerData = spawners.firstOrNull { it.mob.internalName == mythicMob.internalName } ?: return false
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
