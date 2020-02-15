package ink.ptms.sandalphon.module.impl

import ink.ptms.sandalphon.module.IModule
import io.izzel.taboolib.module.command.lite.CommandBuilder
import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.module.lite.SimpleCounter
import io.izzel.taboolib.util.ArrayUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.BlockCommandSender
import org.bukkit.util.NumberConversions
import java.util.concurrent.ConcurrentHashMap

/**
 * @Author sky
 * @Since 2020-02-15 17:32
 */
object CommandBlockControl : IModule() {

    val map = HashMap<Location, Long>()

    @TInject
    val cbc = CommandBuilder.create("CommandBlockControl", null)
            .aliases("cbc")
            .permission("*")
            .execute { sender, args ->
                if (args.size < 2) {
                    notify(sender, "/cbc [period] [command]")
                    return@execute
                }
                if (sender !is BlockCommandSender) {
                    notify(sender, "This command only allows the use of BlockCommand.")
                    return@execute
                }
                if (System.currentTimeMillis() < map[sender.block.location] ?: 0L) {
                    return@execute
                }
                try {
                    Bukkit.dispatchCommand(sender, ArrayUtil.arrayJoin(args, 1))
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                map[sender.block.location] = System.currentTimeMillis() + (NumberConversions.toInt(args[0]) * 50L)
            }
}