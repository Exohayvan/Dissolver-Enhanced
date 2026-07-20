package net.exohayvan.dissolver_enhanced.block;

import com.mojang.serialization.MapCodec;
import java.math.BigInteger;
import net.exohayvan.dissolver_enhanced.block.entity.MaterializerBlockEntity;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MaterializerBlock extends InteractiveMachineBlock<MaterializerBlockEntity> {
    public static final MapCodec<MaterializerBlock> CODEC = simpleCodec(MaterializerBlock::new);

    public MaterializerBlock(Properties settings) {
        super(settings, MaterializerBlockEntity.class, "materializer_block");
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
    public void playerDestroy(
        Level world,
        Player player,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        ItemStack stack
    ) {
        if (!world.isClientSide() && blockEntity instanceof MaterializerBlockEntity materializerBlockEntity) {
            Containers.dropContents(world, pos, materializerBlockEntity);

            BigInteger storedEmc = materializerBlockEntity.getStoredEmcForDrop();
            if (storedEmc.signum() > 0) {
                Containers.dropItemStack(
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    EMCOrbItem.create(storedEmc)
                );
            }
            world.updateNeighbourForOutputSignal(pos, this);
        }
        super.playerDestroy(world, player, pos, state, blockEntity, stack);
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
                ModBlockEntities.MATERIALIZER_BLOCK_ENTITY,
                MaterializerBlockEntity::tick
            );
    }
}
