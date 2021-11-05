package ink.ptms.sandalphon.module.impl.spawner.data

import ink.ptms.sandalphon.module.impl.spawner.ai.FollowAi
import ink.ptms.sandalphon.module.impl.spawner.event.EntityReleaseEvent
import ink.ptms.sandalphon.module.impl.spawner.event.EntitySpawnEvent
import ink.ptms.sandalphon.module.impl.spawner.event.EntityToSpawnEvent
import ink.ptms.sandalphon.module.impl.spawner.event.SpawnerTickEvent
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.mobs.MythicMob
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.metadata.FixedMetadataValue
import taboolib.module.ai.addGoalAi
import taboolib.module.ai.removeGoalAi
import taboolib.platform.BukkitPlugin

/**
 * @author sky
 * @since 2020-05-27 16:03
 */
class SpawnerData(val block: Location, var mob: MythicMob) {

    val mobs = HashMap<Location, LivingEntity>()
    val time = HashMap<Location, Long>()
    val copy = ArrayList<Location>()

    // 激活范围
    var activationrange = 50

    // 活动范围
    var leashrange = 50

    // 复活时间
    var respawn = 60

    fun cancel() {
        mobs.forEach { (_, v) -> v.remove() }
    }

    fun cancel(loc: Location) {
        mobs.remove(loc)?.remove()
    }

    fun tick() {
        tick(block)
        copy.forEach { tick(it) }
    }

    fun tick(loc: Location) {
        if (!SpawnerTickEvent(this).call()) {
            return
        }
        val pos = loc.clone().add(0.5, 1.0, 0.5)
        if (loc.world!!.players.all { it.location.distance(loc) > activationrange }) {
            val entity = mobs.remove(loc) ?: return
            EntityReleaseEvent(entity, this).call()
            entity.remove()
            time[loc] = System.currentTimeMillis() + (respawn * 1000L)
        } else {
            val entity = mobs[loc]
            if (entity != null && (entity.isValid && !entity.hasMetadata("RESPAWN"))) {
                if (entity.location.world!!.name == loc.world!!.name) {
                    if (entity.hasMetadata("SPAWNER_BACKING")) {
                        if (entity.location.distance(pos) < 0.8) {
                            entity.removeMetadata("SPAWNER_BACKING", BukkitPlugin.getInstance())
                            entity.isInvulnerable = false
                            entity.removeGoalAi("FollowAi")
                            EntityToSpawnEvent.Stop(entity, this).call()
                        } else {
                            if (entity is Mob && entity.target != null) {
                                entity.target = null
                            }
                            if (entity.isLeashed) {
                                entity.setLeashHolder(null)
                            }
                            if (entity.isInsideVehicle) {
                                entity.vehicle?.removePassenger(entity)
                            }
                            entity.health = (entity.health + (entity.maxHealth * 0.1)).coerceAtMost(entity.maxHealth)
                        }
                    } else if (entity.location.distance(loc) > leashrange) {
                        entity.setMetadata("SPAWNER_BACKING", FixedMetadataValue(BukkitPlugin.getInstance(), true))
                        entity.isInvulnerable = true
                        entity.addGoalAi(FollowAi(entity, pos, 1.5), 1)
                        EntityToSpawnEvent.Start(entity, this).call()
                    }
                } else {
                    entity.teleport(pos)
                }
            } else {
                val time = time[loc] ?: 0L
                if (time < System.currentTimeMillis()) {
                    val event = EntitySpawnEvent.Pre(this, pos.clone(), time > 0)
                    if (event.call()) {
                        val activeMob = mob.spawn(BukkitAdapter.adapt(event.location), 1.0)
                        mobs[loc] = activeMob.entity.bukkitEntity as LivingEntity
                        EntitySpawnEvent.Post(mobs[loc]!!, this, event.location, time > 0).call()
                    }
                }
            }
        }
    }

    fun isSpawner(block: Block): Boolean {
        return this.block == block.location || block.location in copy
    }
}