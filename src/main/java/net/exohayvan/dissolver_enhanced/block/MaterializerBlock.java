package net.exohayvan.dissolver_enhanced.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.exohayvan.dissolver_enhanced.block.entity.MaterializerBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
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

public class MaterializerBlock extends BlockWithEntity {
    public static final MapCodec<MaterializerBlock> CODEC = createCodec(MaterializerBlock::new);

    public MaterializerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<MaterializerBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MaterializerBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            player.openHandledScreen(materializerBlockEntity);
        }

        return ActionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.MATERIALIZER_BLOCK_ENTITY, MaterializerBlockEntity::tick);
    }
}
