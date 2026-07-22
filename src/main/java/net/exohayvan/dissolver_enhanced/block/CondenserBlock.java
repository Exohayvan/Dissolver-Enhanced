package net.exohayvan.dissolver_enhanced.block;

import org.jetbrains.annotations.Nullable;


import net.exohayvan.dissolver_enhanced.block.entity.CondenserBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class CondenserBlock extends InteractiveMachineBlock {
        public CondenserBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    protected MenuProvider menuProvider(BlockEntity blockEntity) {
        return blockEntity instanceof CondenserBlockEntity entity ? entity : null;
    }

    @Override
    protected String analyticsId() {
        return "condenser_block";
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CONDENSER_BLOCK_ENTITY.get(), CondenserBlockEntity::tick);
    }
}
