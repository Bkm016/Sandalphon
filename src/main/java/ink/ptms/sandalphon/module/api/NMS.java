package ink.ptms.sandalphon.module.api;

import io.izzel.taboolib.module.inject.TInject;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * @Author sky
 * @Since 2020-05-30 17:00
 */
public abstract class NMS {

    @TInject(asm = "ink.ptms.sandalphon.module.api.NMSHandle")
    public static final NMS HANDLE = null;

    abstract public void sendBlockAction(Player player, Block block, int a, int b);

    abstract public void setBlockData(Block block, byte data);

}
