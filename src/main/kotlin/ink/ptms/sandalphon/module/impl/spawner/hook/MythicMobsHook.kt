package ink.ptms.sandalphon.module.impl.spawner.hook

import ink.ptms.sandalphon.module.impl.spawner.Spawner
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.INoTargetSkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Listener
import taboolib.common.platform.event.OptionalEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * @author sky
 * @since 2020-06-13 12:17
 */
class MythicMobsHook : Listener {

    @SubscribeEvent(bind = "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent")
    fun e(e: OptionalEvent) {
        val event = e.get<MythicMechanicLoadEvent>()
        when (event.mechanicName.toLowerCase()) {
            "tospawn" -> event.register(SkillToSpawn(event.mechanicName, event.config))
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