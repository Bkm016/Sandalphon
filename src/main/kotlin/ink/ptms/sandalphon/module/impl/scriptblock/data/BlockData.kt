package ink.ptms.sandalphon.module.impl.scriptblock.data

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.Coerce
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage
import java.util.concurrent.CompletableFuture

class BlockData(
    val block: Location,
    var blockType: BlockType = BlockType.INTERACT,
    var action: MutableList<String> = ArrayList(),
    var condition: MutableList<String> = ArrayList(),
) {

    val link = ArrayList<Location>()

    fun eval(player: Player) {
        try {
            KetherShell.eval(action, sender = adaptPlayer(player))
        } catch (e: Throwable) {
            e.printKetherErrorMessage()
        }
    }

    fun check(player: Player): CompletableFuture<Boolean> {
        return if (condition.isEmpty()) {
            CompletableFuture.completedFuture(true)
        } else {
            try {
                KetherShell.eval(condition, sender = adaptPlayer(player)).thenApply {
                    Coerce.toBoolean(it)
                }
            } catch (e: Throwable) {
                e.printKetherErrorMessage()
                CompletableFuture.completedFuture(false)
            }
        }
    }

    fun isBlock(block: Block): Boolean {
        return this.block == block.location || block.location in link
    }
}