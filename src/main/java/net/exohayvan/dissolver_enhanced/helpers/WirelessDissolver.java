package net.exohayvan.dissolver_enhanced.helpers;

import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.block.entity.DissolverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WirelessDissolver {
    public static int radius = 40;

    public static boolean open(Player player, Level world) {
        Vec3 playerPos = player.position();
        BlockPos startPos = new BlockPos((int) playerPos.x - radius, (int) playerPos.y - radius, (int) playerPos.z - radius);
        BlockPos endPos = new BlockPos((int) playerPos.x + radius, (int) playerPos.y + radius, (int) playerPos.z + radius);

        BlockPos foundPos = findBlockAtPos(world, startPos, endPos);
        if (foundPos == null)  return false;

        BlockEntity blockEntity = world.getBlockEntity(foundPos);
        if (!(blockEntity instanceof DissolverBlockEntity)) return false;

        player.openMenu((DissolverBlockEntity)blockEntity);
        return true;
    }

    private static BlockPos findBlockAtPos(Level world, BlockPos startPos, BlockPos endPos) {
        for (int x = startPos.getX(); x < endPos.getX(); x++) {
        for (int y = startPos.getY(); y < endPos.getY(); y++) {
        for (int z = startPos.getZ(); z < endPos.getZ(); z++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);

            if (state.is(ModBlocks.DISSOLVER_BLOCK)) return pos;
        }
        }
        }

        return null;
    }
}
