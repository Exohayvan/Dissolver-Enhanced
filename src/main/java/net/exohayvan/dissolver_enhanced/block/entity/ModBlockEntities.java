package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;

public class ModBlockEntities {
    public static BlockEntityType<DissolverBlockEntity> DISSOLVER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "dissolver_block_entity",
            BlockEntityType.Builder.of(DissolverBlockEntity::new, ModBlocks.DISSOLVER_BLOCK)
    );
    
    static <T extends BlockEntity> BlockEntityType<T> create(String namespace, String id, Builder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, id), builder.build(null));
    }

    public static void init() {
    }
}
