package net.exohayvan.dissolver_enhanced.block;

import com.mojang.serialization.MapCodec;
import net.exohayvan.dissolver_enhanced.block.entity.CondenserBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CondenserBlock extends InteractiveMachineBlock<CondenserBlockEntity> {
    public static final MapCodec<CondenserBlock> CODEC = simpleCodec(CondenserBlock::new);

    public CondenserBlock(Properties settings) {
        super(settings, CondenserBlockEntity.class, "condenser_block");
    }

    @Override
    protected MapCodec<CondenserBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level world,
        BlockState state,
        BlockEntityType<T> type
    ) {
        return world.isClientSide()
            ? null
            : createTickerHelper(
                type,
                ModBlockEntities.CONDENSER_BLOCK_ENTITY,
                CondenserBlockEntity::tick
            );
    }
}
