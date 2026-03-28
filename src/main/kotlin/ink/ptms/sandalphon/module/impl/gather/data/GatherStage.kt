package ink.ptms.sandalphon.module.impl.gather.data

/**
 * 阶段定义 — 包含方块结构列表、掉落物列表、Kether动作
 * @param duration 该阶段存在时间（秒），0=不自动推进，等待被采集后才推进
 */
data class GatherStage(
    val blocks: MutableList<GatherBlock> = mutableListOf(),
    val drops: MutableList<GatherDrop> = mutableListOf(),
    val ketherActions: MutableList<String> = mutableListOf(),
    var duration: Int = 0
)
