package net.exohayvan.dissolver_enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class CrystalEntity extends Entity {
	public int crystalAge;

	public CrystalEntity(EntityType<? extends CrystalEntity> entityType, Level world) {
		super(entityType, world);
		this.blocksBuilding = true;
		this.crystalAge = this.random.nextInt(100000);
	}

	// public CrystalEntity(World world, double x, double y, double z) {
	// 	this(ModEntities.CRYSTAL_ENTITY_TYPE, world);
	// 	this.setPosition(x, y, z);
	// }

	public boolean isPowered() {
		Vec3 pos = this.position();
		Level world = this.level();
		BlockPos blockPos = new BlockPos((int)pos.x - 1, (int)pos.y - 1, (int)pos.z - 1);

		return world.hasNeighborSignal(blockPos);
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		this.crystalAge++;
		this.checkInsideBlocks();
		// this.isPowered();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return super.shouldRenderAtSqrDistance(distance);
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}
}
