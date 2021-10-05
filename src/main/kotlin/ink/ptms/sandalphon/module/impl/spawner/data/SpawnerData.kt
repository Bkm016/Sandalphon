package ink.ptms.sandalphon.module.impl.spawner.data

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.spawner.Spawner
import ink.ptms.sandalphon.module.impl.spawner.ai.FollowAi
import ink.ptms.sandalphon.module.impl.spawner.event.EntityReleaseEvent
import ink.ptms.sandalphon.module.impl.spawner.event.EntitySpawnEvent
import ink.ptms.sandalphon.module.impl.spawner.event.EntityToSpawnEvent
import ink.ptms.sandalphon.module.impl.spawner.event.SpawnerTickEvent
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.ai.SimpleAiSelector
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.ClickType
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Materials
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.mobs.MythicMob
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author sky
 * @since 2020-05-27 16:03
 */
class SpawnerData(val block: Location, var mob: MythicMob) {

    val mobs = HashMap<Location, LivingEntity>()
    val time = HashMap<Location, Long>()
    val copy = ArrayList<Location>()

    // 激活范围
    var activationrange = 50

    // 活动范围
    var leashrange = 50

    // 复活时间
    var respawn = 60

    fun cancel() {
        mobs.forEach { (_, v) -> v.remove() }
    }

    fun cancel(loc: Location) {
        mobs.remove(loc)?.remove()
    }

    fun tick() {
        tick(block)
        copy.forEach { tick(it) }
    }

    fun tick(loc: Location) {
        if (SpawnerTickEvent(this).call().isCancelled) {
            return
        }
        val pos = loc.clone().add(0.5, 1.0, 0.5)
        if (loc.world!!.players.all { it.location.distance(loc) > activationrange }) {
            val entity = mobs.remove(loc) ?: return
            EntityReleaseEvent(entity, this).call()
            entity.remove()
            time[loc] = System.currentTimeMillis() + (respawn * 1000L)
        } else {
            val entity = mobs[loc]
            if (entity != null && (entity.isValid && !entity.hasMetadata("RESPAWN"))) {
                if (entity.location.world!!.name == loc.world!!.name) {
                    if (entity.hasMetadata("SPAWNER_BACKING")) {
                        if (entity.location.distance(pos) < 0.8) {
                            entity.removeMetadata("SPAWNER_BACKING", Sandalphon.plugin)
                            entity.isInvulnerable = false
                            SimpleAiSelector.getExecutor().removeGoalAi(entity, "FollowAi")
                            EntityToSpawnEvent.Stop(entity, this).call()
                        } else {
                            if (entity is Mob && entity.target != null) {
                                entity.target = null
                            }
                            if (entity.isLeashed) {
                                entity.setLeashHolder(null)
                            }
                            if (entity.isInsideVehicle) {
                                entity.vehicle?.removePassenger(entity)
                            }
                            entity.health = (entity.health + (entity.maxHealth * 0.1)).coerceAtMost(entity.maxHealth)
                        }
                    } else if (entity.location.distance(loc) > leashrange) {
                        entity.setMetadata("SPAWNER_BACKING", FixedMetadataValue(Sandalphon.plugin, true))
                        entity.isInvulnerable = true
                        SimpleAiSelector.getExecutor().addGoalAi(entity, FollowAi(entity, pos, 1.5), 1)
                        EntityToSpawnEvent.Start(entity, this).call()
                    }
                } else {
                    entity.teleport(pos)
                }
            } else {
                val time = time[loc] ?: 0L
                if (time < System.currentTimeMillis()) {
                    val event = EntitySpawnEvent.Pre(this, pos.clone(), time > 0).call()
                    event.nonCancelled {
                        val activeMob = mob.spawn(BukkitAdapter.adapt(event.location), 1.0)
                        mobs[loc] = activeMob.entity.bukkitEntity as LivingEntity
                        EntitySpawnEvent.Post(mobs[loc]!!, this, event.location, time > 0).call()
                    }
                }
            }
        }
    }

    fun isSpawner(block: Block): Boolean {
        return this.block == block.location || block.location in copy
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑刷怪箱 ${Utils.fromLocation(block)}")
                .rows(3)
                .build { inv ->
                    inv.setItem(11, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f激活范围 (${activationrange})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                    inv.setItem(13, ItemBuilder(Materials.PISTON.parseMaterial()).name("§f活动范围 (${leashrange})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                    inv.setItem(15, ItemBuilder(Materials.BONE_BLOCK.parseMaterial()).name("§f复活时间 (${respawn})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                }.event {
                    it.isCancelled = true
                    when (it.rawSlot) {
                        11 -> {
                            if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                activationrange += if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                activationrange -= if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            it.inventory.setItem(11, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f激活范围 (${activationrange})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                        }
                        13 -> {
                            if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                leashrange += if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                leashrange -= if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            it.inventory.setItem(13, ItemBuilder(Materials.PISTON.parseMaterial()).name("§f活动范围 (${leashrange})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                        }
                        15 -> {
                            if (it.clickType == ClickType.CLICK && it.castClick().isLeftClick) {
                                respawn += if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            } else if (it.clickType == ClickType.CLICK && it.castClick().isRightClick) {
                                respawn -= if (it.castClick().isShiftClick) 10 else 1
                                it.clicker.playSound(it.clicker.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
                            }
                            it.inventory.setItem(15, ItemBuilder(Materials.BONE_BLOCK.parseMaterial()).name("§f复活时间 (${respawn})").lore("§7左键 + 1", "§7右键 - 1", "§7SHIFT * 10", "", "§8关闭后生效").build())
                        }
                    }
                }.close {
                    Spawner.export()
                }.open(player)
    }
}