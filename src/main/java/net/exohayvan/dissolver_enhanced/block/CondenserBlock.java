package net.exohayvan.dissolver_enhanced.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.block.entity.CondenserBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.helpers.ActionResultCompat;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CondenserBlock extends BlockWithEntity {
    public static final MapCodec<CondenserBlock> CODEC = createCodec(CondenserBlock::new);

    public CondenserBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<CondenserBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResultCompat.success(true);

        ModAnalytics.captureBlockUse("condenser_block");
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CondenserBlockEntity condenserBlockEntity) {
            player.openHandledScreen(condenserBlockEntity);
        }

        return ActionResultCompat.consume();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.CONDENSER_BLOCK_ENTITY, CondenserBlockEntity::tick);
    }
}
