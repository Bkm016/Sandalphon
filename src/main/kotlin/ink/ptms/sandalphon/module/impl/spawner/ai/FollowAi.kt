package ink.ptms.sandalphon.module.impl.spawner.ai

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import taboolib.common5.Baffle
import taboolib.module.ai.SimpleAi
import taboolib.module.ai.controllerLookAt
import taboolib.module.ai.navigationMove

/**
 * @author sky
 * @since 2018-09-21 9:52
 */
class FollowAi(val entity: LivingEntity, val target: Location, val speed: Double) : SimpleAi() {

    val counter = Baffle.of(10)
    var current = entity.location
    var wait = 0

    override fun shouldExecute(): Boolean {
        return true
    }

    override fun startTask() {
        counter.reset()
    }

    override fun updateTask() {
        entity.controllerLookAt(target.clone().run {
            this.y = entity.eyeLocation.y
            this
        })
        if (counter.hasNext()) {
            if (entity.location.world!!.name == target.world!!.name) {
                // 近距离传送
                if (target.distance(entity.location) < 1.5) {
                    entity.teleport(target)
                }
                // 卡位判定
                else if (current.distance(entity.location) < 1) {
                    if (wait++ >= 10) {
                        entity.teleport(target)
                        return
                    }
                } else {
                    wait = 0
                    current = entity.location
                }
                entity.navigationMove(target, speed)
            } else {
                entity.teleport(target)
            }
        }
    }
}