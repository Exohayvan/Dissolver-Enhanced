package net.exohayvan.dissolver_enhanced.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityType<CrystalEntity> CRYSTAL_ENTITY = EntityType.Builder.of(CrystalEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).build(DissolverEnhanced.MOD_ID + ":crystal_entity");

    // HELPERS

    private static EntityType<?> registerEntityType(String id, EntityType<?> entityType) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(DissolverEnhanced.MOD_ID, id), entityType);
    }

    // INITIALIZE

    public static void init() {
        registerEntityType("crystal_entity", CRYSTAL_ENTITY);
    }
}
