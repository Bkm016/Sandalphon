package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import io.izzel.taboolib.module.event.EventNormal
import org.bukkit.entity.Entity

/**
 * @author sky
 * @since 2020-05-27 22:39
 */
class EntityToSpawnEvent {

    class Start(val entity: Entity, val spawner: SpawnerData) : EventNormal<Start>()

    class Stop(val entity: Entity, val spawner: SpawnerData) : EventNormal<Stop>()
}