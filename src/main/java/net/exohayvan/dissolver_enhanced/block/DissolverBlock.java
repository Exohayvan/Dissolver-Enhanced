package net.exohayvan.dissolver_enhanced.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.exohayvan.dissolver_enhanced.block.entity.DissolverBlockEntity;
import net.exohayvan.dissolver_enhanced.entity.CrystalEntity;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class DissolverBlock extends BaseEntityBlock {
    public DissolverBlock(Properties settings) {
		super(settings);
	}

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        DissolverBlockEntity blockEntity = new DissolverBlockEntity(pos, state);
        return blockEntity;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        spawnEntity(world, pos);
    }
    
    @Override
	public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        if (world instanceof ServerLevel) {
            List<Entity> list = blockEntityList((Level)world, pos);
            if (!list.isEmpty()) list.get(0).remove(Entity.RemovalReason.DISCARDED);
        }
	}

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DissolverBlockEntity) {
                player.openMenu((DissolverBlockEntity)blockEntity);
            }

            // check for entity
            List<Entity> list = blockEntityList(world, pos);
            if (list.isEmpty()) spawnEntity(world, pos);

            return InteractionResult.CONSUME;
        }
    }

    private void spawnEntity(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel)) return;

        CrystalEntity crystalEntity = new CrystalEntity(ModEntities.CRYSTAL_ENTITY, world);
        crystalEntity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        world.addFreshEntity(crystalEntity);
    }

    private List<Entity> blockEntityList(Level world, BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
        return world.getEntities(null, new AABB(x, y, z, x + 1.0F, y + 1.0F, z + 1.0F));
    }

    // PARTICLE
    private float offset = 1.1F;
    private float velocity = 0.01F;
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		super.animateTick(state, world, pos, random);

        if (random.nextInt(12) != 0) return;

        int randomSide = random.nextInt(8);
        boolean northSide = randomSide == 0;
        boolean southSide = randomSide == 1;
        boolean eastSide = randomSide == 2;
        boolean westSide = randomSide == 3;
        boolean topSide = randomSide > 3;

		double x = eastSide ? offset : westSide ? 0 : Math.random();
		double y = topSide ? offset : Math.random();
		double z = southSide ? offset : northSide ? 0 : Math.random();
        
		world.addParticle(ParticleTypes.END_ROD, pos.getX() + x, pos.getY() + y, pos.getZ() + z, eastSide ? velocity : westSide ? -velocity : 0, topSide ? velocity : 0, southSide ? velocity : northSide ? -velocity : 0);
	}
}
