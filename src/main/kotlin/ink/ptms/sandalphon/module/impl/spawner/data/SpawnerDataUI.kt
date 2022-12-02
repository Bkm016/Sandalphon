package ink.ptms.sandalphon.module.impl.spawner.data

import ink.ptms.sandalphon.module.impl.spawner.Spawner
import ink.ptms.sandalphon.util.Utils
import org.bukkit.Sound
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.ClickType
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildItem

@Suppress("DuplicatedCode")
fun SpawnerData.openEdit(player: Player) {
    fun build1() = buildItem(XMaterial.OBSERVER) {
        name = "§f激活范围 (${activationrange})"
        lore += listOf("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效")
    }

    fun build2() = buildItem(XMaterial.PISTON) {
        name = "§f活动范围 (${leashrange})"
        lore += listOf("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效")
    }

    fun build3() = buildItem(XMaterial.BONE_BLOCK) {
        name = "§f复活时间 (${respawn})"
        lore += listOf("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效")
    }
    player.openMenu<Basic>("编辑刷怪箱 ${Utils.fromLocation(block)}") {
        rows(3)
        onBuild { _, inv ->
            inv.setItem(11, build1())
            inv.setItem(13, build2())
            inv.setItem(15, build3())
        }
        onClick(lock = true) {
            it.isCancelled = true
            when (it.rawSlot) {
                11 -> {
                    if (it.clickType == ClickType.CLICK && it.clickEvent().isLeftClick) {
                        activationrange += if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    } else if (it.clickType == ClickType.CLICK && it.clickEvent().isRightClick) {
                        activationrange -= if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    it.inventory.setItem(11, build1())
                }
                13 -> {
                    if (it.clickType == ClickType.CLICK && it.clickEvent().isLeftClick) {
                        leashrange += if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    } else if (it.clickType == ClickType.CLICK && it.clickEvent().isRightClick) {
                        leashrange -= if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    it.inventory.setItem(13, build2())
                }
                15 -> {
                    if (it.clickType == ClickType.CLICK && it.clickEvent().isLeftClick) {
                        respawn += if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    } else if (it.clickType == ClickType.CLICK && it.clickEvent().isRightClick) {
                        respawn -= if (it.clickEvent().isShiftClick) 10 else 1
                        it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                    }
                    it.inventory.setItem(15, build3())
                }
            }
        }
        onClose {
            Spawner.export()
        }
    }
}