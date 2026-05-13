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

    public static BlockEntityType<CondenserBlockEntity> CONDENSER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "condenser_block_entity",
            FabricBlockEntityTypeBuilder.create(CondenserBlockEntity::new, ModBlocks.CONDENSER_BLOCK)
    );

    public static BlockEntityType<MaterializerBlockEntity> MATERIALIZER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "materializer_block_entity",
            FabricBlockEntityTypeBuilder.create(MaterializerBlockEntity::new, ModBlocks.MATERIALIZER_BLOCK)
    );
    
    static <T extends BlockEntity> BlockEntityType<T> create(String namespace, String id, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(namespace, id), builder.build());
    }

    public static void init() {
    }
}
