package ink.ptms.sandalphon.module.impl.spawner.hook

//class MythicMobsHook : Listener {
//
//    @SubscribeEvent(bind = "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent")
//    fun e(e: OptionalEvent) {
//        val event = e.get<MythicMechanicLoadEvent>()
//        when (event.mechanicName.toLowerCase()) {
//            "tospawn" -> event.register(SkillToSpawn(event.mechanicName, event.config))
//        }
//    }
//}
//
//class SkillToSpawn(skill: String, mlc: MythicLineConfig) : SkillMechanic(skill, mlc), INoTargetSkill {
//
//    override fun cast(skillMetadata: SkillMetadata): Boolean {
//        val entity = skillMetadata.caster.entity.bukkitEntity
//        if (entity is LivingEntity) {
//            return Spawner.toSpawn(entity)
//        }
//        return false
//    }
//}