package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<DissolverBlockEntity> DISSOLVER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "dissolver_block_entity",
            FabricBlockEntityTypeBuilder.create(DissolverBlockEntity::new, ModBlocks.DISSOLVER_BLOCK)
    );
    
    static <T extends BlockEntity> BlockEntityType<T> create(String namespace, String id, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(namespace, id), builder.build());
    }

    public static void init() {
    }
}
