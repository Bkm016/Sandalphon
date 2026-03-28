package ink.ptms.sandalphon.module.impl.gather.data

/**
 * 采集方式
 */
enum class GatherType(val display: String) {
    LEFT("左键破坏"),
    RIGHT("右键读条"),
    BOTH("左键+右键")
}

/**
 * 工具检查方式
 */
enum class ToolCheckType(val display: String) {
    ID("Zaphkiel物品ID"),
    LORE("Lore包含"),
    NONE("无限制")
}
