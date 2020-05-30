package ink.ptms.sandalphon.module.api;

import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Blocks;
import net.minecraft.server.v1_15_R1.PacketPlayOutBlockAction;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * @Author sky
 * @Since 2020-05-30 17:00
 */
public class NMSHandle extends NMS {

    @Override
    public void sendBlockAction(Player player, Block block, int a, int b) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutBlockAction(new BlockPosition(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ()), Blocks.CHEST, a, b));
    }
}
