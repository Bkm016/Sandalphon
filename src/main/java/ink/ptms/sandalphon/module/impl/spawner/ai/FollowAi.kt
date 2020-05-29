package ink.ptms.sandalphon.module.impl.spawner.ai

import io.izzel.taboolib.module.ai.SimpleAi
import io.izzel.taboolib.module.ai.SimpleAiSelector
import io.izzel.taboolib.module.lite.SimpleCounter
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

/**
 * @Author sky
 * @Since 2018-09-21 9:52
 */
class FollowAi(val entity: LivingEntity, val target: Location, val speed: Double) : SimpleAi() {

    val counter = SimpleCounter(10)
    var current = entity.location
    var wait = 0

    override fun shouldExecute(): Boolean {
        return true
    }

    override fun startTask() {
        counter.reset()
        SimpleAiSelector.getExecutor().setFollowRange(entity, 100.0)
    }

    override fun resetTask() {
        SimpleAiSelector.getExecutor().setPathEntity(entity, null)
    }

    override fun updateTask() {
        SimpleAiSelector.getExecutor().controllerLookAt(entity, target.clone().run {
            this.y = entity.eyeLocation.y
            this
        })
        if (counter.next()) {
            if (entity.location.world!!.name == target.world!!.name) {
                if (current.distance(entity.location) < 1) {
                    if (wait++ >= 10) {
                        entity.teleport(target)
                        return
                    }
                } else {
                    wait = 0
                    current = entity.location
                }
                SimpleAiSelector.getExecutor().navigationMove(entity, target, speed)
            } else {
                entity.teleport(target)
            }
        }
    }
}