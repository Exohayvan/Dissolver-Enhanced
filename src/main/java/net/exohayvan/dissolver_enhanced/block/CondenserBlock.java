package net.exohayvan.dissolver_enhanced.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.exohayvan.dissolver_enhanced.block.entity.CondenserBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CondenserBlock extends ScreenOpeningBlock {
    public static final MapCodec<CondenserBlock> CODEC = createCodec(CondenserBlock::new);

    public CondenserBlock(Settings settings) {
        super(settings, "condenser_block");
    }

    @Override
    protected MapCodec<CondenserBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        World world,
        BlockState state,
        BlockEntityType<T> type
    ) {
        return world.isClient()
            ? null
            : validateTicker(type, ModBlockEntities.CONDENSER_BLOCK_ENTITY, CondenserBlockEntity::tick);
    }
}
