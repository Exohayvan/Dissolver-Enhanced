package net.exohayvan.dissolver_enhanced.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModEntities {
    public static final EntityType<CrystalEntity> CRYSTAL_ENTITY = EntityType.Builder.create(CrystalEntity::new, SpawnGroup.MISC).dimensions(0.5F, 0.5F).build();

    // HELPERS

    private static EntityType<?> registerEntityType(String id, EntityType<?> entityType) {
		return Registry.register(Registries.ENTITY_TYPE, Identifier.of(DissolverEnhanced.MOD_ID, id), entityType);
    }

    // INITIALIZE

    public static void init() {
        registerEntityType("crystal_entity", CRYSTAL_ENTITY);
    }
}
