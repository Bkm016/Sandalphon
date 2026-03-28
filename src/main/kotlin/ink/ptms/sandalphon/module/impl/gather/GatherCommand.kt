package ink.ptms.sandalphon.module.impl.gather

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common5.Coerce
import taboolib.expansion.createHelper
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import taboolib.platform.util.giveItem
import ink.ptms.sandalphon.module.impl.gather.data.GatherInstance
import ink.ptms.sandalphon.module.impl.gather.data.GatherPoint
import ink.ptms.sandalphon.module.impl.gather.data.GatherStage
import ink.ptms.sandalphon.module.impl.gather.hook.GatherHologram

@CommandHeader(name = "gather", aliases = ["采集"], permission = "gather.admin")
object GatherCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            GatherManager.reload()
            sender.success("采集系统已重载. 模板: ${GatherManager.points.size}, 实例: ${GatherManager.locationIndex.size}")
        }
    }

    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, _, _ ->
            if (GatherManager.points.isEmpty()) {
                sender.info("暂无采集模板.")
                return@execute
            }
            sender.info("采集模板列表:")
            GatherManager.points.values.forEach { point ->
                val count = GatherManager.instances[point.id]?.size ?: 0
                sender.info("§8 - §f${point.id} §7(${point.gatherType.display}) §8实例: $count")
            }
        }
    }

    @CommandBody
    val create = subCommand {
        dynamic("id") {
            execute<Player> { sender, _, argument ->
                // 如果模板已存在，直接给魔杖
                val existing = GatherManager.points[argument]
                if (existing != null) {
                    sender.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                        name = "§f§f§f场景魔杖"
                        lore += listOf("§7Gather", "§7${existing.id}")
                        shiny()
                    })
                    sender.info("已获得 §f${existing.id}§7 的场景魔杖. 左键放置, 右键删除.")
                    return@execute
                }
                // 模板不存在，在线创建新模板
                val point = GatherPoint(argument)
                point.stages.add(GatherStage()) // 默认给一个空阶段
                GatherManager.points[point.id] = point
                GatherManager.rebuildCache()
                sender.success("采集模板 §f${point.id}§7 已创建.")
                // 给场景魔杖
                sender.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                    name = "§f§f§f场景魔杖"
                    lore += listOf("§7Gather", "§7${point.id}")
                    shiny()
                })
                sender.info("已获得场景魔杖. 左键放置, 右键删除.")
                sender.info("使用 §f/gather edit ${point.id}§7 打开GUI编辑.")
                // 直接打开编辑界面
                GatherEditUI.openMainEdit(sender, point)
            }
        }
    }

    @CommandBody
    val debug = subCommand {
        dynamic("id") {
            suggestion<CommandSender> { _, _ ->
                GatherManager.points.keys.toList()
            }
            execute<Player> { sender, _, argument ->
                val point = GatherManager.points[argument]
                if (point == null) {
                    sender.error("模板 '$argument' 不存在.")
                    return@execute
                }
                sender.giveItem(buildItem(XMaterial.STICK) {
                    name = "§f§f§f调试魔杖"
                    lore += listOf("§7Gather", "§7${point.id}")
                    shiny()
                })
                sender.info("已获得 §f${point.id}§7 的调试魔杖. 左键重建, 右键切换阶段.")
            }
        }
    }

    @CommandBody
    val capture = subCommand {
        dynamic("id") {
            suggestion<CommandSender> { _, _ ->
                GatherManager.points.keys.toList()
            }
            dynamic("stage") {
                execute<Player> { sender, context, argument ->
                    val pointId = context["id"]
                    val point = GatherManager.points[pointId]
                    if (point == null) {
                        sender.error("模板 '$pointId' 不存在.")
                        return@execute
                    }
                    val stageIdx = argument.toIntOrNull()
                    if (stageIdx == null || stageIdx < 0 || stageIdx >= point.stages.size) {
                        sender.error("阶段索引无效. 可用: 0~${point.stages.size - 1}")
                        return@execute
                    }
                    sender.giveItem(buildItem(XMaterial.BLAZE_ROD) {
                        name = "§f§f§f捕获魔杖"
                        lore += listOf("§7Gather", "§7$pointId $stageIdx")
                        shiny()
                    })
                    sender.info("已获得捕获魔杖. 左键选起点, 右键选终点, 丢弃完成捕获.")
                }
            }
        }
    }

    @CommandBody
    val edit = subCommand {
        dynamic("id") {
            suggestion<CommandSender> { _, _ ->
                GatherManager.points.keys.toList()
            }
            execute<Player> { sender, _, argument ->
                val point = GatherManager.points[argument]
                if (point == null) {
                    sender.error("模板 '$argument' 不存在.")
                    return@execute
                }
                GatherEditUI.openMainEdit(sender, point)
            }
        }
    }

    @CommandBody
    val near = subCommand {
        dynamic("range", optional = true) {
            execute<Player> { sender, _, argument ->
                val range = argument.toDoubleOrNull() ?: 50.0
                sender.info("附近采集点 (${range}m):")
                var count = 0
                GatherManager.instances.values.flatten().forEach { inst ->
                    if (inst.location.world?.name == sender.world.name
                        && inst.location.distance(sender.location) < range
                    ) {
                        val dist = Coerce.format(inst.location.distance(sender.location))
                        sender.info("§8 - §f${inst.pointId} §7阶段${inst.currentStage} §8(${dist}m) §7生长:${if (inst.needGrow) "§c等待" else "§a完成"}")
                        count++
                    }
                }
                if (count == 0) sender.info("§7无.")
            }
        }
        execute<Player> { sender, _, _ ->
            val range = 50.0
            sender.info("附近采集点 (${range}m):")
            var count = 0
            GatherManager.instances.values.flatten().forEach { inst ->
                if (inst.location.world?.name == sender.world.name
                    && inst.location.distance(sender.location) < range
                ) {
                    val dist = Coerce.format(inst.location.distance(sender.location))
                    sender.info("§8 - §f${inst.pointId} §7阶段${inst.currentStage} §8(${dist}m) §7生长:${if (inst.needGrow) "§c等待" else "§a完成"}")
                    count++
                }
            }
            if (count == 0) sender.info("§7无.")
        }
    }

    @CommandBody
    val hologram = subCommand {
        literal("offset") {
            dynamic("x") {
                dynamic("y") {
                    dynamic("z") {
                        execute<Player> { sender, context, _ ->
                            val block = sender.getTargetBlockExact(10)
                            if (block == null || block.type == Material.AIR) {
                                sender.error("无效的方块.")
                                return@execute
                            }
                            val inst = GatherManager.findByLocation(block.location)
                            if (inst == null) {
                                sender.error("该位置不存在采集点实例.")
                                return@execute
                            }
                            val x = context["x"].toDoubleOrNull() ?: 0.0
                            val y = context["y"].toDoubleOrNull() ?: 0.0
                            val z = context["z"].toDoubleOrNull() ?: 0.0
                            inst.hologramOffset = listOf(x, y, z)
                            val point = GatherManager.points[inst.pointId]
                            if (point != null) {
                                GatherHologram.updateLocation(inst, point)
                            }
                            sender.success("全息偏移已设置为 ($x, $y, $z)")
                            GatherManager.saveInstances()
                        }
                    }
                }
            }
        }
        literal("toggle") {
            execute<Player> { sender, _, _ ->
                val block = sender.getTargetBlockExact(10)
                if (block == null || block.type == Material.AIR) {
                    sender.error("无效的方块.")
                    return@execute
                }
                val inst = GatherManager.findByLocation(block.location)
                if (inst == null) {
                    sender.error("该位置不存在采集点实例.")
                    return@execute
                }
                val point = GatherManager.points[inst.pointId] ?: return@execute
                val current = inst.hologramEnable ?: point.hologramEnable
                inst.hologramEnable = !current
                // 刷新该模板下所有实例
                GatherHologram.refreshAll(point)
                sender.success("全息显示已${if (!current) "开启" else "关闭"}")
                GatherManager.saveInstances()
            }
        }
    }

    @CommandBody
    val save = subCommand {
        execute<CommandSender> { sender, _, _ ->
            GatherManager.saveInstances()
            sender.success("采集数据已保存.")
        }
    }

    @CommandBody
    val migrate = GatherMigration
}
