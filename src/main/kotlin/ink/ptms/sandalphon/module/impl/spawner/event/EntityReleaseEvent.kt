package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import org.bukkit.entity.Entity
import taboolib.platform.type.BukkitProxyEvent

/**
 * @author sky
 * @since 2020-05-27 22:39
 */
class EntityReleaseEvent(val entity: Entity, val spawner: SpawnerData) : BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false
}