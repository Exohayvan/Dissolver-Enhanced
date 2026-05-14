package net.exohayvan.dissolver_enhanced.block;

import org.jetbrains.annotations.Nullable;


import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.block.entity.CondenserBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class CondenserBlock extends BaseEntityBlock {
        public CondenserBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CondenserBlockEntity condenserBlockEntity) {
            ModAnalytics.captureBlockUse("condenser_block");
            player.openMenu(condenserBlockEntity);
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CONDENSER_BLOCK_ENTITY.get(), CondenserBlockEntity::tick);
    }
}
