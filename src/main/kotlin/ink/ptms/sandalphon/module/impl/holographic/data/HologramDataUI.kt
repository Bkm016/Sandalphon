package ink.ptms.sandalphon.module.impl.holographic.data

import ink.ptms.sandalphon.module.impl.holographic.Hologram
import org.bukkit.Sound
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.ClickType
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildBook
import taboolib.platform.util.buildItem
import taboolib.platform.util.giveItem

fun HologramData.openEdit(player: Player) {
    player.openMenu<Basic>("编辑全息 $id") {
        rows(3)
        set(11, buildItem(XMaterial.PISTON) {
            name = "§f移动"
            lore += listOf("§7左键 + 0.1", "§7右键 - 0.1", "", "§8关闭后生效")
        })
        set(13, buildItem(XMaterial.BOOKSHELF) {
            name = "§f内容"
            lore += content.map { "§7$it" }
        })
        set(15, buildItem(XMaterial.OBSERVER) {
            name = "§f条件"
            lore += condition.map { "§7$it" }
        })
        onClick(11) {
            if (it.clickType == ClickType.CLICK && it.clickEvent().isLeftClick) {
                location.add(0.0, 0.1, 0.0)
                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
            } else if (it.clickType == ClickType.CLICK && it.clickEvent().isRightClick) {
                location.subtract(0.0, 0.1, 0.0)
                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
            }
            init()
        }
        onClick(13) {
            player.closeInventory()
            player.giveItem(buildBook {
                material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
                write(content.joinToString("\n"))
                name = "§f§f§f编辑内容"
                lore += listOf("§7Hologram", "§7$id")
            })
        }
        onClick(15) {
            player.closeInventory()
            player.giveItem(buildBook {
                material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
                write(content.joinToString("\n"))
                name = "§f§f§f编辑条件"
                lore += listOf("§7Hologram", "§7$id")
            })
        }
        onClose(once = false) {
            Hologram.export()
            init()
        }
    }
}