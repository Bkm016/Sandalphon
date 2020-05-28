package ink.ptms.sandalphon.module.impl.spawner.data

import ink.ptms.sandalphon.module.impl.spawner.event.EntityReleaseEvent
import ink.ptms.sandalphon.module.impl.spawner.event.EntityRespawnEvent
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.mobs.ActiveMob
import io.lumine.xikage.mythicmobs.mobs.MythicMob
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity

/**
 * @Author sky
 * @Since 2020-05-27 16:03
 */
class SpawnerData(val block: Location, val mob: MythicMob) {

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

    fun tick() {
        tick(block)
        copy.forEach { tick(it) }
    }

    fun tick(loc: Location) {
        if (loc.world!!.players.all { it.location.distance(loc) > activationrange }) {
            val entity = mobs.remove(loc) ?: return
            EntityReleaseEvent(entity, this).call()
            entity.remove()
        } else {
            val entity = mobs[loc]
            if (entity != null && entity.isValid) {
                return
            }
            val time = time[loc] ?: 0L
            if (time > System.currentTimeMillis()) {
                return
            }
            val activeMob = mob.spawn(BukkitAdapter.adapt(loc.clone().add(0.0, 1.0, 0.0)), 1)
            if (time > 0) {
                EntityRespawnEvent(activeMob.livingEntity, this, loc).call()
            }
            mobs[loc] = activeMob.livingEntity
        }
    }

    fun isSpawner(block: Block): Boolean {
        return this.block == block.location || block.location in copy
    }
}