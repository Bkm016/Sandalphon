package ink.ptms.sandalphon.module.api;

import io.izzel.taboolib.Version;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Blocks;
import net.minecraft.server.v1_16_R1.PacketPlayOutBlockAction;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * @author sky
 * @since 2020-05-30 17:00
 */
public class NMSHandle extends NMS {

    private static final int version = Version.getCurrentVersionInt();

    @Override
    public void sendBlockAction(Player player, Block block, int a, int b) {
        if (version >= 11300) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutBlockAction(new BlockPosition(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ()), Blocks.CHEST, a, b));
        } else {
            ((org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer) player).getHandle().playerConnection.sendPacket(new net.minecraft.server.v1_12_R1.PacketPlayOutBlockAction(new net.minecraft.server.v1_12_R1.BlockPosition(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ()), net.minecraft.server.v1_12_R1.Blocks.CHEST, a, b));
        }
    }

    @Override
    public void setBlockData(Block block, byte data) {
        ((CraftBlock) block).setData(data, false);
    }
}
