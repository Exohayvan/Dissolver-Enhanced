package net.exohayvan.dissolver_enhanced.block;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;



import net.exohayvan.dissolver_enhanced.block.entity.MaterializerBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MaterializerBlock extends InteractiveMachineBlock {
        public MaterializerBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterializerBlockEntity(pos, state);
    }

    protected MenuProvider menuProvider(BlockEntity blockEntity) {
        return blockEntity instanceof MaterializerBlockEntity entity ? entity : null;
    }

    @Override
    protected String analyticsId() {
        return "materializer_block";
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
