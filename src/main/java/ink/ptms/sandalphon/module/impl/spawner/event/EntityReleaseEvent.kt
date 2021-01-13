package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import io.izzel.taboolib.module.event.EventNormal
import org.bukkit.entity.Entity

/**
 * @Author sky
 * @Since 2020-05-27 22:39
 */
class EntityReleaseEvent(val entity: Entity, val spawner: SpawnerData) : EventNormal<EntityReleaseEvent>()