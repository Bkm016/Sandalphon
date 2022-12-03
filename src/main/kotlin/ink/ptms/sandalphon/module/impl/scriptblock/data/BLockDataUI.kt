package ink.ptms.sandalphon.module.impl.scriptblock.data

import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.util.ItemBuilder
import ink.ptms.sandalphon.util.Utils
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildBook
import taboolib.platform.util.giveItem

fun BlockData.openEdit(player: Player) {
    player.openMenu<Basic>("编辑脚本 ${Utils.fromLocation(block)}") {
        rows(3)
        onBuild { _, inv ->
            inv.setItem(11, ItemBuilder(XMaterial.DAYLIGHT_DETECTOR).name("§f触发方式").lore("§7${blockType.display}").build())
            inv.setItem(13, ItemBuilder(XMaterial.PISTON).name("§f动作").lore(action.map { "§7$it" }).build())
            inv.setItem(15, ItemBuilder(XMaterial.OBSERVER).name("§f条件").lore(condition.map { "§7$it" }).build())
        }
        onClick(lock = true) {
            it.isCancelled = true
            when (it.rawSlot) {
                11 -> {
                    blockType = when (blockType) {
                        BlockType.INTERACT -> BlockType.WALK
                        BlockType.WALK -> BlockType.PROJECTILE
                        else -> BlockType.INTERACT
                    }
                    openEdit(player)
                }
                13 -> {
                    player.closeInventory()
                    player.giveItem(buildBook {
                        material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
                        write(action.joinToString("\n"))
                        name = "§f§f§f编辑动作"
                        lore += listOf("§7ScriptBlock", "§7${Utils.fromLocation(block)}")
                    })
                }
                15 -> {
                    player.closeInventory()
                    player.giveItem(buildBook {
                        material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
                        write(condition.joinToString("\n"))
                        name = "§f§f§f编辑条件"
                        lore += listOf("§7ScriptBlock", "§7${Utils.fromLocation(block)}")
                    })
                }
            }
        }
        onClose(once = false) {
            ScriptBlock.export()
        }
    }
}