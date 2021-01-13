package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import io.izzel.taboolib.module.event.EventCancellable
import io.izzel.taboolib.module.event.EventNormal
import org.bukkit.Location
import org.bukkit.entity.LivingEntity

/**
 * @Author sky
 * @Since 2020-05-27 22:39
 */
class EntitySpawnEvent {

    class Pre(val spawner: SpawnerData, var location: Location, val isRespawn: Boolean) : EventCancellable<Pre>()

    class Post(val entity: LivingEntity, val spawner: SpawnerData, val location: Location, val isRespawn: Boolean) : EventNormal<Pre>()
}