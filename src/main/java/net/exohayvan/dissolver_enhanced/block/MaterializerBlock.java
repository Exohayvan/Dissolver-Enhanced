package net.exohayvan.dissolver_enhanced.block;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.block.entity.MaterializerBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.helpers.ActionResultCompat;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
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
        if (world.isClient()) return ActionResultCompat.success(true);

        ModAnalytics.captureBlockUse("materializer_block");
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            player.openHandledScreen(materializerBlockEntity);
        }

        return ActionResultCompat.consume();
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
                ItemScatterer.spawn(world, pos, materializerBlockEntity);

                BigInteger storedEmc = materializerBlockEntity.getStoredEmcForDrop();
                if (storedEmc.signum() > 0) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), EMCOrbItem.create(storedEmc));
                }

                world.updateComparators(pos, this);
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.MATERIALIZER_BLOCK_ENTITY, MaterializerBlockEntity::tick);
    }
}
