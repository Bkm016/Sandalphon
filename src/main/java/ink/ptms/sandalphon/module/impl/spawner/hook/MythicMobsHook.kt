package ink.ptms.sandalphon.module.impl.spawner.hook

import ink.ptms.sandalphon.module.impl.spawner.Spawner
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.module.inject.TSchedule
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @Author sky
 * @Since 2020-06-13 12:17
 */
@TListener(depend = ["MythicMobs"])
class MythicMobsHook : Listener {

    @EventHandler
    fun e(e: MythicMechanicLoadEvent) {
        when (e.mechanicName.toLowerCase()) {
            "tospawn" -> {
                e.register(SkillToSpawn(e.mechanicName, e.config))
            }
        }
    }

    class SkillToSpawn(skill: String, mlc: MythicLineConfig) : SkillMechanic(skill, mlc), INoTargetSkill {

        override fun cast(skillMetadata: SkillMetadata): Boolean {
            val entity = skillMetadata.caster.entity.bukkitEntity
            if (entity is LivingEntity) {
                return Spawner.toSpawn(entity)
            }
            return false
        }
    }
}