package ink.ptms.sandalphon.module.impl.gather.hook

import eu.decentsoftware.holograms.api.DHAPI
import org.bukkit.Location
import taboolib.common.platform.function.warning
import ink.ptms.sandalphon.module.impl.gather.GatherManager
import ink.ptms.sandalphon.module.impl.gather.data.GatherInstance
import ink.ptms.sandalphon.module.impl.gather.data.GatherPoint

/**
 * DecentHolograms 全息管理
 * 使用临时模式（不持久化到DHD配置文件）
 * 全息位置 = 锚点方块上方1格中心 + offset
 */
object GatherHologram {

    private const val PREFIX = "Gather_"

    /**
     * 获取全息ID
     */
    fun hologramId(instance: GatherInstance): String {
        val loc = instance.location
        return "${PREFIX}${loc.world?.name}_${loc.blockX}_${loc.blockY}_${loc.blockZ}"
    }

    /**
     * 计算全息位置
     */
    fun hologramLocation(instance: GatherInstance, point: GatherPoint): Location {
        val offset = instance.hologramOffset ?: point.hologramOffset
        val ox = offset.getOrElse(0) { 0.0 }
        val oy = offset.getOrElse(1) { 0.0 }
        val oz = offset.getOrElse(2) { 0.0 }
        return instance.location.clone().add(0.5 + ox, 1.0 + oy, 0.5 + oz)
    }

    /**
     * 获取显示内容（空内容时显示采集点ID）
     */
    fun getLines(instance: GatherInstance, point: GatherPoint): List<String> {
        return if (point.hologramLines.isNotEmpty()) {
            point.hologramLines
        } else {
            listOf("&a[&f${point.id}&a]")
        }
    }

    /**
     * 创建全息（临时模式，saveToFile=false）
     */
    fun create(instance: GatherInstance, point: GatherPoint) {
        val enable = instance.hologramEnable ?: point.hologramEnable
        if (!enable) return

        val id = hologramId(instance)
        val loc = hologramLocation(instance, point)
        val lines = getLines(instance, point)

        try {
            remove(instance)
            // saveToFile = false → 临时模式
            DHAPI.createHologram(id, loc, false, lines)
        } catch (e: Exception) {
            warning("[Gather] 创建全息失败: ${e.message}")
        }
    }

    /**
     * 移除全息
     */
    fun remove(instance: GatherInstance) {
        try {
            val id = hologramId(instance)
            DHAPI.getHologram(id)?.delete()
        } catch (_: Exception) {
        }
    }

    /**
     * 隐藏全息（方块被破坏时）
     */
    fun hide(instance: GatherInstance) {
        remove(instance)
    }

    /**
     * 显示全息（方块恢复时）
     */
    fun show(instance: GatherInstance, point: GatherPoint) {
        create(instance, point)
    }

    /**
     * 清理所有采集点全息
     */
    fun clearAll() {
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram.getCachedHolograms().toList().forEach {
                if (it.name.startsWith(PREFIX)) {
                    it.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 更新全息位置
     */
    fun updateLocation(instance: GatherInstance, point: GatherPoint) {
        remove(instance)
        create(instance, point)
    }

    /**
     * 刷新模板下所有实例的全息（开关/内容变更时调用）
     */
    fun refreshAll(point: GatherPoint) {
        val instances = GatherManager.instances[point.id] ?: return
        val enable = point.hologramEnable
        for (inst in instances) {
            val instEnable = inst.hologramEnable ?: enable
            if (instEnable && !inst.needGrow) {
                remove(inst)
                create(inst, point)
            } else {
                remove(inst)
            }
        }
    }
}
