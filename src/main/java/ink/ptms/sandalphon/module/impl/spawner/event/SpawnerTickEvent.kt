package ink.ptms.sandalphon.module.impl.spawner.event

import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import io.izzel.taboolib.module.event.EventCancellable

/**
 * @author sky
 * @since 2020-05-27 22:39
 */
class SpawnerTickEvent(val spawner: SpawnerData) : EventCancellable<SpawnerTickEvent>()