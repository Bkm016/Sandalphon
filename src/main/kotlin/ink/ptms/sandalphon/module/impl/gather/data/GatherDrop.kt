package ink.ptms.sandalphon.module.impl.gather.data

/**
 * 掉落物定义
 * @param item 物品ID — 支持 Zaphkiel ID 或原版 Material 名称（如 DIAMOND, IRON_INGOT）
 * @param amount 数量
 * @param chance 概率 0.0~1.0
 * @param displayName 自定义显示名称（原版物品时生效，空=使用采集点名称）
 * @param onPickupActions 拾取后执行的 Kether 动作列表（空=不执行）
 */
data class GatherDrop(
    var item: String = "",
    var amount: Int = 1,
    var chance: Double = 1.0,
    var displayName: String = "",
    var onPickupActions: MutableList<String> = mutableListOf()
)
