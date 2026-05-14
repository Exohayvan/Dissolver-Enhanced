package net.exohayvan.dissolver_enhanced.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<EntityType<CrystalEntity>> CRYSTAL_ENTITY = ENTITIES.register(
            "crystal_entity",
            () -> EntityType.Builder.of(CrystalEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build(DissolverEnhanced.MOD_ID + ":crystal_entity")
    );

    public static void init(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
