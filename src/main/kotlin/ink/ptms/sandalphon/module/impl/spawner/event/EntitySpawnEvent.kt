package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

/**
 * @author sky
 * @since 2020-05-27 22:39
 */
class EntitySpawnEvent {

    class Pre(val spawner: SpawnerData, var location: Location, val isRespawn: Boolean) : BukkitProxyEvent()

    class Post(val entity: LivingEntity, val spawner: SpawnerData, val location: Location, val isRespawn: Boolean) : BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}