package net.exohayvan.dissolver_enhanced.block;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MaterializerBlock extends BaseEntityBlock {
    public static final MapCodec<MaterializerBlock> CODEC = simpleCodec(MaterializerBlock::new);

    public MaterializerBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<MaterializerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterializerBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            ModAnalytics.captureBlockUse("materializer_block");
            player.openMenu(materializerBlockEntity);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack) {
        if (!world.isClientSide() && blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            Containers.dropContents(world, pos, materializerBlockEntity);

            BigInteger storedEmc = materializerBlockEntity.getStoredEmcForDrop();
            if (storedEmc.signum() > 0) {
                Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), EMCOrbItem.create(storedEmc));
            }

            world.updateNeighbourForOutputSignal(pos, this);
        }

        super.playerDestroy(world, player, pos, state, blockEntity, stack);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MATERIALIZER_BLOCK_ENTITY, MaterializerBlockEntity::tick);
    }
}
