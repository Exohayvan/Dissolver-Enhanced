package net.exohayvan.dissolver_enhanced.block;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;


import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.block.entity.MaterializerBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MaterializerBlock extends BaseEntityBlock {
        public MaterializerBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterializerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            ModAnalytics.captureBlockUse("materializer_block");
            player.openMenu(materializerBlockEntity);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            Containers.dropContents(world, pos, materializerBlockEntity);

            BigInteger storedEmc = materializerBlockEntity.getStoredEmcForDrop();
            if (storedEmc.signum() > 0) {
                Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), EMCOrbItem.create(storedEmc));
            }

            world.updateNeighbourForOutputSignal(pos, this);
        }

        super.onRemove(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MATERIALIZER_BLOCK_ENTITY.get(), MaterializerBlockEntity::tick);
    }
}
