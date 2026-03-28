package ink.ptms.sandalphon.module.impl.gather.data

import ink.ptms.adyeshach.core.entity.EntityTypes

/**
 * 采集点模板定义（从YAML配置加载）
 */
class GatherPoint(val id: String) {

    /** 采集方式 */
    var gatherType: GatherType = GatherType.RIGHT

    /** 右键读条时间（tick） */
    var gatherTime: Int = 40

    /** 工具要求（Zaphkiel物品ID），空=无限制 */
    var tool: String = ""

    /** 工具检查方式 */
    var toolCheck: ToolCheckType = ToolCheckType.NONE

    /** 生长恢复时间（秒） */
    var growTime: Int = 60

    /** 生长恢复概率 0.0~1.0 */
    var growChance: Double = 1.0

    /** 全息显示开关 */
    var hologramEnable: Boolean = false

    /** 全息偏移（相对于被破坏方块上方1格的额外偏移） */
    var hologramOffset: List<Double> = listOf(0.0, 0.0, 0.0)

    /** 全息显示内容 */
    var hologramLines: MutableList<String> = mutableListOf()

    /** NPC模式开关 */
    var npcEnable: Boolean = false

    /** NPC类型 */
    var npcType: EntityTypes = EntityTypes.VILLAGER

    /** NPC名称 */
    var npcName: String = ""

    /** NPC是否看向玩家 */
    var npcLookAtPlayer: Boolean = true

    /** 阶段列表 */
    val stages: MutableList<GatherStage> = mutableListOf()
}
