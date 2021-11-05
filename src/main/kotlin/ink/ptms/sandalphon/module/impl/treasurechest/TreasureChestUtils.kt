package ink.ptms.sandalphon.module.impl.treasurechest

import org.bukkit.Material

fun Material.isTreasureType(): Boolean {
    return name == "CHEST" || name == "TRAPPED_CHEST" || name == "ENDER_CHEST" || name == "BARREL" || name.endsWith("SHULKER_BOX")
}