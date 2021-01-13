package ink.ptms.sandalphon.module.impl.scriptblock.data

import ink.ptms.cronus.internal.condition.Condition
import ink.ptms.cronus.internal.condition.ConditionParser
import ink.ptms.cronus.internal.program.QuestEffect
import ink.ptms.sandalphon.module.impl.scriptblock.ScriptBlock
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.cronus.CronusUtils
import io.izzel.taboolib.util.book.builder.BookBuilder
import io.izzel.taboolib.util.item.ItemBuilder
import io.izzel.taboolib.util.item.inventory.MenuBuilder
import io.izzel.taboolib.util.lite.Materials
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player

class BlockData(val block: Location, var blockType: BlockType = BlockType.INTERACT, var blockAction: MutableList<String> = ArrayList(), var blockCondition: MutableList<String> = ArrayList()) {

    val link = ArrayList<Location>()
    lateinit var action: QuestEffect
    lateinit var condition: List<Condition>

    init {
        init()
    }

    fun init() {
        action = QuestEffect(blockAction)
        condition = blockCondition.map { ConditionParser.parse(it) }
    }

    fun eval(player: Player) {
        action.eval(player)
    }

    fun check(player: Player): Boolean {
        return condition.all { it.check(player) }
    }

    fun isBlock(block: Block): Boolean {
        return this.block == block.location || block.location in link
    }

    fun openEdit(player: Player) {
        MenuBuilder.builder()
                .title("编辑脚本 ${Utils.fromLocation(block)}")
                .rows(3)
                .build { inv ->
                    inv.setItem(11, ItemBuilder(Materials.DAYLIGHT_DETECTOR.parseMaterial()).name("§f触发方式").lore("§7${blockType.display}").build())
                    inv.setItem(13, ItemBuilder(Materials.PISTON.parseMaterial()).name("§f动作").lore(blockAction.map { "§7$it" }).build())
                    inv.setItem(15, ItemBuilder(Materials.OBSERVER.parseMaterial()).name("§f条件").lore(blockCondition.map { "§7$it" }).build())
                }.event {
                    it.isCancelled = true
                    when (it.rawSlot) {
                        11 -> {
                            blockType = if (blockType == BlockType.INTERACT) {
                                BlockType.WALK
                            } else {
                                BlockType.INTERACT
                            }
                            openEdit(player)
                        }
                        13 -> {
                            player.closeInventory()
                            CronusUtils.addItem(player, ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(blockAction.joinToString("\n")).build()).name("§f§f§f编辑动作").lore("§7ScriptBlock", "§7${Utils.fromLocation(block)}").build())
                        }
                        15 -> {
                            player.closeInventory()
                            CronusUtils.addItem(player, ItemBuilder(BookBuilder(Materials.WRITABLE_BOOK.parseItem()).pagesRaw(blockAction.joinToString("\n")).build()).name("§f§f§f编辑条件").lore("§7ScriptBlock", "§7${Utils.fromLocation(block)}").build())
                        }
                    }
                }.close {
                    ScriptBlock.export()
                }.open(player)
    }
}