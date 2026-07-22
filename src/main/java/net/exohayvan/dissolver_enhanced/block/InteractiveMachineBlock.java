package net.exohayvan.dissolver_enhanced.block;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

abstract class InteractiveMachineBlock extends BaseEntityBlock {
    protected InteractiveMachineBlock(Properties settings) {
        super(settings);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    protected abstract MenuProvider menuProvider(BlockEntity blockEntity);

    protected abstract String analyticsId();

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        MenuProvider provider = menuProvider(world.getBlockEntity(pos));
        if (provider != null) {
            ModAnalytics.captureBlockUse(analyticsId());
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }
}