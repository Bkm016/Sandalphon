package ink.ptms.sandalphon.module.impl.gather.data

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Location
import java.util.*

/**
 * 已放置的采集点实例（运行时状态）
 */
class GatherInstance(
    /** 所属模板ID */
    val pointId: String,
    /** 锚点位置 */
    val location: Location
) {
    /** 当前阶段索引 */
    var currentStage: Int = 0

    /** 上次生长时间戳(ms) */
    var lastGrowTime: Long = 0L

    /** 是否需要生长恢复（被破坏后标记） */
    var needGrow: Boolean = false

    /** 全息偏移覆盖（null=使用模板默认值） */
    var hologramOffset: List<Double>? = null

    /** 全息是否启用覆盖（null=使用模板默认值） */
    var hologramEnable: Boolean? = null

    /** NPC实体引用（NPC模式下） */
    @Transient
    var npcEntity: EntityInstance? = null

    /** NPC UUID（NPC模式下，用于事件匹配） */
    var npcUUID: UUID? = null

    /** 位置索引 key */
    fun locationKey(): String {
        return toLocationKey(location)
    }

    companion object {
        fun toLocationKey(loc: Location): String {
            return "${loc.world?.name},${loc.blockX},${loc.blockY},${loc.blockZ}"
        }
    }
}
