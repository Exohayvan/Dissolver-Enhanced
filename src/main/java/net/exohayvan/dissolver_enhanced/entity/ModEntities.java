package net.exohayvan.dissolver_enhanced.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.helpers.RegistryKeyCompat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalEntity>> CRYSTAL_ENTITY = ENTITIES.register(
            "crystal_entity",
            () -> RegistryKeyCompat.buildEntityType(
                    "crystal_entity",
                    EntityType.Builder.of(CrystalEntity::new, MobCategory.MISC).sized(0.5F, 0.5F)
            )
    );

    public static void init(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
