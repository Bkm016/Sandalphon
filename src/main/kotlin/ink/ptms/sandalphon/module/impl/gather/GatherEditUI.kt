package ink.ptms.sandalphon.module.impl.gather

import ink.ptms.sandalphon.Sandalphon
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import taboolib.common.platform.function.submit
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.ifAir
import taboolib.module.nms.inputSign
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Chest
import taboolib.platform.util.buildBook
import taboolib.platform.util.buildItem
import taboolib.platform.util.giveItem
import ink.ptms.sandalphon.module.impl.gather.data.*
import ink.ptms.sandalphon.module.impl.gather.hook.GatherHologram

/**
 * 采集点 GUI 编辑界面
 */
object GatherEditUI {

    /**
     * 主编辑界面
     */
    fun openMainEdit(player: Player, point: GatherPoint) {
        player.openMenu<Basic>("编辑采集点 ${point.id}") {
            rows(4)
            onBuild { _, inv ->
                // 采集方式
                inv.setItem(10, buildItem(XMaterial.DIAMOND_PICKAXE) {
                    name = "§f采集方式"
                    lore += listOf(
                        "§7当前: §e${point.gatherType.display}",
                        "",
                        "§8左键切换"
                    )
                })
                // 读条时间
                inv.setItem(11, buildItem(XMaterial.CLOCK) {
                    name = "§f读条时间"
                    lore += listOf(
                        "§7当前: §e${point.gatherTime} tick §8(${point.gatherTime / 20.0}秒)",
                        "",
                        "§8左键编辑"
                    )
                })
                // 工具要求
                inv.setItem(12, buildItem(XMaterial.IRON_PICKAXE) {
                    name = "§f工具要求"
                    lore += listOf(
                        "§7工具: §e${point.tool.ifEmpty { "无" }}",
                        "§7检查: §e${point.toolCheck.display}",
                        "",
                        "§8左键编辑工具ID",
                        "§8右键切换检查方式"
                    )
                })
                // 生长设置
                inv.setItem(14, buildItem(XMaterial.OAK_SAPLING) {
                    name = "§f生长恢复"
                    lore += listOf(
                        "§7时间: §e${point.growTime}秒",
                        "§7概率: §e${(point.growChance * 100).toInt()}%",
                        "",
                        "§8左键编辑时间",
                        "§8右键编辑概率"
                    )
                })
                // 全息设置
                inv.setItem(15, buildItem(XMaterial.END_ROD) {
                    name = "§f全息显示"
                    lore += listOf(
                        "§7启用: §e${if (point.hologramEnable) "§a是" else "§c否"}",
                        "§7偏移: §e${point.hologramOffset.joinToString(", ")}",
                        "§7内容:",
                        *point.hologramLines.map { "§7  $it" }.toTypedArray(),
                        "",
                        "§8左键切换开关",
                        "§8右键编辑内容"
                    )
                })
                // NPC设置
                inv.setItem(16, buildItem(XMaterial.VILLAGER_SPAWN_EGG) {
                    name = "§fNPC模式"
                    lore += listOf(
                        "§7启用: §e${if (point.npcEnable) "§a是" else "§c否"}",
                        "§7类型: §e${point.npcType.name}",
                        "§7名称: §e${point.npcName.ifEmpty { "无" }}",
                        "",
                        "§8左键切换开关",
                        "§8右键编辑名称"
                    )
                })
                // 阶段编辑
                inv.setItem(22, buildItem(XMaterial.BOOKSHELF) {
                    name = "§f阶段管理"
                    lore += listOf(
                        "§7阶段数: §e${point.stages.size}",
                        *point.stages.mapIndexed { i, s ->
                            "§7  第${i}阶段: ${s.blocks.size}方块, ${s.drops.size}掉落"
                        }.toTypedArray(),
                        "",
                        "§8点击编辑"
                    )
                })
            }
            onClick(lock = true) {
                when (it.rawSlot) {
                    10 -> {
                        point.gatherType = when (point.gatherType) {
                            GatherType.LEFT -> GatherType.RIGHT
                            GatherType.RIGHT -> GatherType.BOTH
                            GatherType.BOTH -> GatherType.LEFT
                        }
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openMainEdit(player, point)
                    }
                    11 -> {
                        player.closeInventory()
                        player.inputSign(arrayOf("${point.gatherTime}", "", "输入读条时间(tick)", "")) { sign ->
                            point.gatherTime = sign[0].toIntOrNull() ?: point.gatherTime
                            openMainEdit(player, point)
                        }
                    }
                    12 -> {
                        if (it.clickEvent().isLeftClick) {
                            player.closeInventory()
                            player.inputSign(arrayOf(point.tool, "", "输入物品ID", "")) { sign ->
                                point.tool = sign[0]
                                openMainEdit(player, point)
                            }
                        } else if (it.clickEvent().isRightClick) {
                            point.toolCheck = when (point.toolCheck) {
                                ToolCheckType.NONE -> ToolCheckType.ID
                                ToolCheckType.ID -> ToolCheckType.LORE
                                ToolCheckType.LORE -> ToolCheckType.NONE
                            }
                            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                            openMainEdit(player, point)
                        }
                    }
                    14 -> {
                        if (it.clickEvent().isLeftClick) {
                            player.closeInventory()
                            player.inputSign(arrayOf("${point.growTime}", "", "输入恢复时间(秒)", "")) { sign ->
                                point.growTime = sign[0].toIntOrNull() ?: point.growTime
                                openMainEdit(player, point)
                            }
                        } else if (it.clickEvent().isRightClick) {
                            player.closeInventory()
                            player.inputSign(arrayOf("${point.growChance}", "", "输入恢复概率(0.0~1.0)", "")) { sign ->
                                point.growChance = sign[0].toDoubleOrNull() ?: point.growChance
                                openMainEdit(player, point)
                            }
                        }
                    }
                    15 -> {
                        if (it.clickEvent().isLeftClick) {
                            point.hologramEnable = !point.hologramEnable
                            // 刷新所有实例的全息
                            GatherHologram.refreshAll(point)
                            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                            openMainEdit(player, point)
                        } else if (it.clickEvent().isRightClick) {
                            openHologramEdit(player, point)
                        }
                    }
                    16 -> {
                        if (it.clickEvent().isLeftClick) {
                            point.npcEnable = !point.npcEnable
                            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                            openMainEdit(player, point)
                        } else if (it.clickEvent().isRightClick) {
                            player.closeInventory()
                            player.inputSign(arrayOf(point.npcName, "", "输入NPC名称", "")) { sign ->
                                point.npcName = sign[0]
                                openMainEdit(player, point)
                            }
                        }
                    }
                    22 -> {
                        openStageList(player, point)
                    }
                }
            }
            onClose {
                GatherManager.saveInstances()
            }
        }
    }

    /**
     * 全息编辑界面（内容编辑 + 位置调整）
     */
    fun openHologramEdit(player: Player, point: GatherPoint) {
        player.openMenu<Basic>("编辑全息 ${point.id}") {
            rows(5)
            onBuild { _, inv ->
                // 第一行: 全息内容行
                point.hologramLines.forEachIndexed { i, line ->
                    if (i < 7) {
                        inv.setItem(i, buildItem(XMaterial.PAPER) {
                            name = "§f第${i + 1}行"
                            lore += listOf("§7$line", "", "§8左键编辑", "§c丢弃删除")
                        })
                    }
                }
                inv.setItem(7, buildItem(XMaterial.LIME_DYE) {
                    name = "§a添加行"
                })
                inv.setItem(8, buildItem(XMaterial.BARRIER) {
                    name = "§c清空内容"
                    lore += "§7清空后将显示采集点ID"
                })

                // 分隔线
                for (s in 9..17) {
                    inv.setItem(s, buildItem(XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " })
                }

                // 当前偏移显示
                val offset = point.hologramOffset
                val ox = offset.getOrElse(0) { 0.0 }
                val oy = offset.getOrElse(1) { 0.0 }
                val oz = offset.getOrElse(2) { 0.0 }
                inv.setItem(13, buildItem(XMaterial.COMPASS) {
                    name = "§f当前偏移"
                    lore += listOf("§7X: §e$ox", "§7Y: §e$oy", "§7Z: §e$oz", "", "§8点击重置为 0,0,0")
                })

                // Y轴 (上下)
                inv.setItem(18, buildItem(XMaterial.RED_WOOL) { name = "§cY -1.0"; lore += "§7向下移动 1.0" })
                inv.setItem(19, buildItem(XMaterial.PINK_WOOL) { name = "§cY -0.5"; lore += "§7向下移动 0.5" })
                inv.setItem(20, buildItem(XMaterial.LIME_WOOL) { name = "§aY +0.5"; lore += "§7向上移动 0.5" })
                inv.setItem(21, buildItem(XMaterial.GREEN_WOOL) { name = "§aY +1.0"; lore += "§7向上移动 1.0" })

                // X轴 (东西)
                inv.setItem(27, buildItem(XMaterial.RED_WOOL) { name = "§cX -1.0"; lore += "§7向西移动 1.0" })
                inv.setItem(28, buildItem(XMaterial.PINK_WOOL) { name = "§cX -0.5"; lore += "§7向西移动 0.5" })
                inv.setItem(29, buildItem(XMaterial.LIME_WOOL) { name = "§aX +0.5"; lore += "§7向东移动 0.5" })
                inv.setItem(30, buildItem(XMaterial.GREEN_WOOL) { name = "§aX +1.0"; lore += "§7向东移动 1.0" })

                // Z轴 (南北)
                inv.setItem(33, buildItem(XMaterial.RED_WOOL) { name = "§cZ -1.0"; lore += "§7向北移动 1.0" })
                inv.setItem(34, buildItem(XMaterial.PINK_WOOL) { name = "§cZ -0.5"; lore += "§7向北移动 0.5" })
                inv.setItem(35, buildItem(XMaterial.LIME_WOOL) { name = "§aZ +0.5"; lore += "§7向南移动 0.5" })
                inv.setItem(36, buildItem(XMaterial.GREEN_WOOL) { name = "§aZ +1.0"; lore += "§7向南移动 1.0" })

                // 返回
                inv.setItem(44, buildItem(XMaterial.ARROW) { name = "§7返回" })
            }
            onClick(lock = true) {
                val slot = it.rawSlot
                // 内容行编辑
                if (slot in 0 until point.hologramLines.size.coerceAtMost(7)) {
                    if (it.clickEvent().click == ClickType.DROP) {
                        point.hologramLines.removeAt(slot)
                        GatherHologram.refreshAll(point)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openHologramEdit(player, point)
                    } else if (it.clickEvent().isLeftClick) {
                        player.closeInventory()
                        player.inputSign(arrayOf(point.hologramLines[slot], "", "编辑全息行", "")) { sign ->
                            point.hologramLines[slot] = sign[0]
                            GatherHologram.refreshAll(point)
                            openHologramEdit(player, point)
                        }
                    }
                    return@onClick
                }
                when (slot) {
                    7 -> {
                        player.closeInventory()
                        player.inputSign(arrayOf("", "", "输入新行内容", "")) { sign ->
                            if (sign[0].isNotEmpty()) {
                                point.hologramLines.add(sign[0])
                                GatherHologram.refreshAll(point)
                            }
                            openHologramEdit(player, point)
                        }
                    }
                    8 -> {
                        point.hologramLines.clear()
                        GatherHologram.refreshAll(point)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openHologramEdit(player, point)
                    }
                    13 -> {
                        // 重置偏移
                        point.hologramOffset = listOf(0.0, 0.0, 0.0)
                        GatherHologram.refreshAll(point)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openHologramEdit(player, point)
                    }
                    // Y轴
                    18 -> adjustOffset(player, point, 1, -1.0)
                    19 -> adjustOffset(player, point, 1, -0.5)
                    20 -> adjustOffset(player, point, 1, 0.5)
                    21 -> adjustOffset(player, point, 1, 1.0)
                    // X轴
                    27 -> adjustOffset(player, point, 0, -1.0)
                    28 -> adjustOffset(player, point, 0, -0.5)
                    29 -> adjustOffset(player, point, 0, 0.5)
                    30 -> adjustOffset(player, point, 0, 1.0)
                    // Z轴
                    33 -> adjustOffset(player, point, 2, -1.0)
                    34 -> adjustOffset(player, point, 2, -0.5)
                    35 -> adjustOffset(player, point, 2, 0.5)
                    36 -> adjustOffset(player, point, 2, 1.0)
                    // 返回
                    44 -> openMainEdit(player, point)
                }
            }
            onClose {
                GatherManager.saveInstances()
            }
        }
    }

    private fun adjustOffset(player: Player, point: GatherPoint, axis: Int, delta: Double) {
        val current = point.hologramOffset.toMutableList()
        while (current.size < 3) current.add(0.0)
        current[axis] = ((current[axis] + delta) * 10).toLong() / 10.0
        point.hologramOffset = current
        GatherHologram.refreshAll(point)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
        openHologramEdit(player, point)
    }

    fun openStageList(player: Player, point: GatherPoint) {
        player.openMenu<Basic>("阶段管理 ${point.id}") {
            rows(4)
            onBuild { _, inv ->
                point.stages.forEachIndexed { i, stage ->
                    if (i < 18) {
                        inv.setItem(i, buildItem(XMaterial.PAPER) {
                            name = "§f阶段 $i"
                            lore += listOf(
                                "§7方块: ${stage.blocks.size}",
                                "§7掉落: ${stage.drops.size}",
                                "§7动作: ${stage.ketherActions.size}",
                                "",
                                "§8左键编辑",
                                "§8右键获取捕获魔杖",
                                "§c丢弃删除"
                            )
                        })
                    }
                }
                inv.setItem(18, buildItem(XMaterial.LIME_DYE) { name = "§a新增阶段" })
                inv.setItem(27, buildItem(XMaterial.ARROW) { name = "§e前进一位"; lore += "§7将选中阶段向前移动一位" })
                inv.setItem(28, buildItem(XMaterial.ARROW) { name = "§e后退一位"; lore += "§7将选中阶段向后移动一位" })
                inv.setItem(29, buildItem(XMaterial.SPECTRAL_ARROW) { name = "§6置顶"; lore += "§7将选中阶段移到最前" })
                inv.setItem(30, buildItem(XMaterial.SPECTRAL_ARROW) { name = "§6置底"; lore += "§7将选中阶段移到最后" })
                val sel = stageSelection[player.uniqueId]
                if (sel != null && sel < point.stages.size) {
                    inv.setItem(31, buildItem(XMaterial.ENDER_EYE) { name = "§f已选中: §e阶段 $sel"; lore += "§7点击取消选中" })
                } else {
                    inv.setItem(31, buildItem(XMaterial.ENDER_PEARL) { name = "§7未选中阶段"; lore += "§7先Shift+左键一个阶段来选中" })
                }
                inv.setItem(35, buildItem(XMaterial.BARRIER) { name = "§7返回" })
            }
            onClick(lock = true) {
                if (it.rawSlot in 0 until point.stages.size) {
                    if (it.clickEvent().click == ClickType.DROP) {
                        point.stages.removeAt(it.rawSlot)
                        stageSelection.remove(player.uniqueId)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openStageList(player, point)
                    } else if (it.clickEvent().isShiftClick && it.clickEvent().isLeftClick) {
                        stageSelection[player.uniqueId] = it.rawSlot
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f)
                        openStageList(player, point)
                    } else if (it.clickEvent().isRightClick) {
                        player.closeInventory()
                        player.inventory.addItem(buildItem(XMaterial.BLAZE_ROD) {
                            name = "§f§f§f捕获魔杖"
                            lore += listOf("§7Gather", "§7${point.id} ${it.rawSlot}")
                            shiny()
                        })
                        player.info("使用捕获魔杖: 左键选起点, 右键选终点, 丢弃完成捕获.")
                    } else if (it.clickEvent().isLeftClick) {
                        openStageEdit(player, point, it.rawSlot)
                    }
                } else when (it.rawSlot) {
                    18 -> { point.stages.add(GatherStage()); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openStageList(player, point) }
                    27 -> { val sel = stageSelection[player.uniqueId] ?: return@onClick; if (sel > 0 && sel < point.stages.size) { swapStages(point, sel, sel - 1); stageSelection[player.uniqueId] = sel - 1; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f) }; openStageList(player, point) }
                    28 -> { val sel = stageSelection[player.uniqueId] ?: return@onClick; if (sel >= 0 && sel < point.stages.size - 1) { swapStages(point, sel, sel + 1); stageSelection[player.uniqueId] = sel + 1; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f) }; openStageList(player, point) }
                    29 -> { val sel = stageSelection[player.uniqueId] ?: return@onClick; if (sel > 0 && sel < point.stages.size) { val stage = point.stages.removeAt(sel); point.stages.add(0, stage); stageSelection[player.uniqueId] = 0; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f) }; openStageList(player, point) }
                    30 -> { val sel = stageSelection[player.uniqueId] ?: return@onClick; if (sel >= 0 && sel < point.stages.size - 1) { val stage = point.stages.removeAt(sel); point.stages.add(stage); stageSelection[player.uniqueId] = point.stages.size - 1; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f) }; openStageList(player, point) }
                    31 -> { stageSelection.remove(player.uniqueId); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openStageList(player, point) }
                    35 -> openMainEdit(player, point)
                }
            }
            onClose { GatherManager.saveInstances() }
        }
    }

    private val stageSelection = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Int>()

    private fun swapStages(point: GatherPoint, a: Int, b: Int) {
        val tmp = point.stages[a]; point.stages[a] = point.stages[b]; point.stages[b] = tmp
    }

    fun openStageEdit(player: Player, point: GatherPoint, stageIndex: Int) {
        val stage = point.stages.getOrNull(stageIndex) ?: return
        player.openMenu<Basic>("编辑阶段 $stageIndex - ${point.id}") {
            rows(3)
            onBuild { _, inv ->
                inv.setItem(10, buildItem(XMaterial.STONE) { name = "§f方块结构"; lore += listOf("§7数量: §e${stage.blocks.size}", *stage.blocks.mapIndexed { i, b -> "§7  $i: ${b.material.name} → ${b.replace.name}" }.toTypedArray(), "", "§8点击编辑") })
                inv.setItem(12, buildItem(XMaterial.CHEST) { name = "§f掉落物"; lore += listOf("§7数量: §e${stage.drops.size}", *stage.drops.map { d -> "§7  ${d.item} x${d.amount} §8(${(d.chance * 100).toInt()}%)" }.toTypedArray(), "", "§8点击编辑") })
                inv.setItem(14, buildItem(XMaterial.COMMAND_BLOCK) { name = "§fKether动作"; lore += listOf("§7数量: §e${stage.ketherActions.size}", *stage.ketherActions.map { "§7  $it" }.toTypedArray(), "", "§8点击编辑") })
                inv.setItem(16, buildItem(XMaterial.CLOCK) { name = "§f阶段存在时间"; lore += listOf("§7当前: §e${when { stage.duration > 0 -> "${stage.duration}秒后自动推进"; stage.duration < 0 -> "采集后立刻恢复"; else -> "不自动推进" }}", "", "§7 0 = 不自动推进(等待采集+growTime恢复)", "§7>0 = 到时间自动切换到下一阶段", "§7<0 = 采集后立刻恢复到阶段0", "", "§8点击编辑") })
                inv.setItem(22, buildItem(XMaterial.ARROW) { name = "§7返回" })
            }
            onClick(lock = true) {
                when (it.rawSlot) {
                    10 -> openBlockList(player, point, stageIndex)
                    12 -> openDropList(player, point, stageIndex)
                    14 -> openKetherEdit(player, point, stageIndex)
                    16 -> { player.closeInventory(); player.inputSign(arrayOf("${stage.duration}", "", "存在时间(秒) 0=不推进", "")) { sign -> stage.duration = sign[0].toIntOrNull() ?: stage.duration; openStageEdit(player, point, stageIndex) } }
                    22 -> openStageList(player, point)
                }
            }
            onClose { GatherManager.saveInstances() }
        }
    }

    fun openBlockList(player: Player, point: GatherPoint, stageIndex: Int) {
        val stage = point.stages.getOrNull(stageIndex) ?: return
        player.openMenu<Basic>("方块结构 阶段$stageIndex - ${point.id}") {
            rows(4)
            onBuild { _, inv ->
                stage.blocks.forEachIndexed { i, block ->
                    if (i < 27) {
                        inv.setItem(i, buildItem(XMaterial.matchXMaterial(block.material) ?: XMaterial.STONE) {
                            name = "§f方块 $i"
                            lore += listOf("§7材质: §e${block.material.name}", "§7替换: §e${block.replace.name}", "§7偏移: §e${block.offset.joinToString(", ")}", "§7朝向: §e${block.direction.name}", "", "§8左键编辑材质", "§8右键编辑替换材质", "§c丢弃删除")
                        })
                    }
                }
                inv.setItem(27, buildItem(XMaterial.LIME_DYE) { name = "§a添加方块" })
                inv.setItem(35, buildItem(XMaterial.ARROW) { name = "§7返回" })
            }
            onClick(lock = true) {
                if (it.rawSlot in 0 until stage.blocks.size) {
                    val block = stage.blocks[it.rawSlot]
                    if (it.clickEvent().click == ClickType.DROP) { stage.blocks.removeAt(it.rawSlot); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openBlockList(player, point, stageIndex) }
                    else if (it.clickEvent().isLeftClick) { player.closeInventory(); player.inputSign(arrayOf(block.material.name, "", "输入材质名", "")) { sign -> val mat = runCatching { Material.valueOf(sign[0].uppercase()) }.getOrNull(); if (mat != null) block.material = mat; GatherManager.rebuildCache(); openBlockList(player, point, stageIndex) } }
                    else if (it.clickEvent().isRightClick) { player.closeInventory(); player.inputSign(arrayOf(block.replace.name, "", "输入替换材质名", "")) { sign -> val mat = runCatching { Material.valueOf(sign[0].uppercase()) }.getOrNull(); if (mat != null) block.replace = mat; GatherManager.rebuildCache(); openBlockList(player, point, stageIndex) } }
                } else if (it.rawSlot == 27) { stage.blocks.add(GatherBlock()); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openBlockList(player, point, stageIndex) }
                else if (it.rawSlot == 35) { openStageEdit(player, point, stageIndex) }
            }
            onClose { GatherManager.saveInstances() }
        }
    }

    fun openDropList(player: Player, point: GatherPoint, stageIndex: Int) {
        val stage = point.stages.getOrNull(stageIndex) ?: return
        player.openMenu<Basic>("掉落物 阶段$stageIndex - ${point.id}") {
            rows(4)
            onBuild { _, inv ->
                stage.drops.forEachIndexed { i, drop ->
                    if (i < 27) {
                        inv.setItem(i, buildItem(XMaterial.GOLD_NUGGET) {
                            name = "§f掉落 $i"
                            lore += listOf("§7物品: §e${drop.item}", "§7名称: §e${drop.displayName.ifEmpty { "默认" }}", "§7数量: §e${drop.amount}", "§7概率: §e${(drop.chance * 100).toInt()}%", "§7拾取动作: §e${drop.onPickupActions.size}条", "", "§8左键放入物品识别ID", "§8右键编辑数量/概率/名称", "§8Shift+右键手动输入ID", "§8Shift+左键编辑拾取动作", "§c丢弃删除")
                        })
                    }
                }
                inv.setItem(27, buildItem(XMaterial.LIME_DYE) { name = "§a添加掉落" })
                inv.setItem(35, buildItem(XMaterial.ARROW) { name = "§7返回" })
            }
            onClick(lock = true) {
                if (it.rawSlot in 0 until stage.drops.size) {
                    val drop = stage.drops[it.rawSlot]
                    val slotIndex = it.rawSlot
                    if (it.clickEvent().click == ClickType.DROP) { stage.drops.removeAt(slotIndex); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openDropList(player, point, stageIndex) }
                    else if (it.clickEvent().isShiftClick && it.clickEvent().isLeftClick) { openPickupActionEdit(player, point, stageIndex, slotIndex) }
                    else if (it.clickEvent().isShiftClick && it.clickEvent().isRightClick) { player.closeInventory(); player.inputSign(arrayOf(drop.item, "", "输入物品ID", "")) { sign -> drop.item = sign[0]; openDropList(player, point, stageIndex) } }
                    else if (it.clickEvent().isLeftClick) { player.closeInventory(); openItemIdentify(player, drop) { openDropList(player, point, stageIndex) } }
                    else if (it.clickEvent().isRightClick) { player.closeInventory(); player.inputSign(arrayOf("${drop.amount}", "${drop.chance}", drop.displayName, "数量/概率/名称")) { sign -> drop.amount = sign[0].toIntOrNull() ?: drop.amount; drop.chance = sign[1].toDoubleOrNull() ?: drop.chance; drop.displayName = sign[2]; openDropList(player, point, stageIndex) } }
                } else if (it.rawSlot == 27) { stage.drops.add(GatherDrop()); player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 2f); openDropList(player, point, stageIndex) }
                else if (it.rawSlot == 35) { openStageEdit(player, point, stageIndex) }
            }
            onClose { GatherManager.saveInstances() }
        }
    }

    fun openKetherEdit(player: Player, point: GatherPoint, stageIndex: Int) {
        val stage = point.stages.getOrNull(stageIndex) ?: return
        player.closeInventory()
        player.giveItem(buildBook {
            material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
            write(stage.ketherActions.joinToString("\n"))
            name = "§f§f§f编辑Kether动作"
            lore += listOf("§7Gather", "§7${point.id}", "§7kether", "§7$stageIndex")
        })
        player.info("在书中编辑Kether动作, 每行一条. 写完后签名保存. 写 §fclear§7 清空.")
    }

    fun openPickupActionEdit(player: Player, point: GatherPoint, stageIndex: Int, dropIndex: Int) {
        val stage = point.stages.getOrNull(stageIndex) ?: return
        val drop = stage.drops.getOrNull(dropIndex) ?: return
        player.closeInventory()
        player.giveItem(buildBook {
            material = XMaterial.WRITABLE_BOOK.parseMaterial()!!
            write(drop.onPickupActions.joinToString("\n"))
            name = "§f§f§f编辑拾取动作"
            lore += listOf("§7Gather", "§7${point.id}", "§7pickup", "§7$stageIndex", "§7$dropIndex")
        })
        player.info("在书中编辑拾取Kether动作, 每行一条. 写完后签名保存. 写 §fclear§7 清空.")
    }

    /**
     * 放入物品识别ID界面 (使用Zaphkiel)
     */
    fun openItemIdentify(player: Player, drop: GatherDrop, callback: () -> Unit) {
        submit(delay = 2) {
            player.openMenu<Chest>("放入物品识别") {
                map("####@####")
                handLocked(false)
                set('#', buildItem(XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " }) { isCancelled = true }
                onClick(lock = false)
                onClose { event ->
                    val item = event.inventory.getItem(4).ifAir()
                    if (item == null) {
                        player.info("未放入物品, 保持原ID: §f${drop.item}")
                        submit(delay = 2) { callback() }
                        return@onClose
                    }
                    // 归还物品
                    player.giveItem(item)
                    // 优先识别 Zaphkiel ID
                    val zaphId = Sandalphon.itemAPI?.getId(item)

                    if (zaphId != null) {
                        drop.item = zaphId
                        drop.amount = item.amount
                        player.success("识别为 Zaphkiel: §f$zaphId §7x${item.amount}")
                    } else {
                        drop.item = item.type.name
                        drop.amount = item.amount
                        player.success("识别为原版物品: §f${item.type.name} §7x${item.amount}")
                    }
                    submit(delay = 2) { callback() }
                }
            }
        }
    }
}
