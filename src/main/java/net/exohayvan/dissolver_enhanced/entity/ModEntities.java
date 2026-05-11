package net.exohayvan.dissolver_enhanced.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    private static final ResourceKey<EntityType<?>> CRYSTAL_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "crystal_entity"));
    public static final EntityType<CrystalEntity> CRYSTAL_ENTITY = EntityType.Builder.of(CrystalEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).build(CRYSTAL_ENTITY_KEY);

    // HELPERS

    private static EntityType<?> registerEntityType(String id, EntityType<?> entityType) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id), entityType);
    }

    // INITIALIZE

    public static void init() {
        registerEntityType("crystal_entity", CRYSTAL_ENTITY);
    }
}
