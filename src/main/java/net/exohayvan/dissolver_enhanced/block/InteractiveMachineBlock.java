package net.exohayvan.dissolver_enhanced.block;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

abstract class InteractiveMachineBlock<T extends BlockEntity & MenuProvider> extends BaseEntityBlock {
    private final Class<T> blockEntityClass;
    private final String analyticsId;

    protected InteractiveMachineBlock(Properties settings, Class<T> blockEntityClass, String analyticsId) {
        super(settings);
        this.blockEntityClass = blockEntityClass;
        this.analyticsId = analyticsId;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntityClass.isInstance(blockEntity)) {
            ModAnalytics.captureBlockUse(analyticsId);
            player.openMenu(blockEntityClass.cast(blockEntity));
        }
        return InteractionResult.CONSUME;
    }
}
