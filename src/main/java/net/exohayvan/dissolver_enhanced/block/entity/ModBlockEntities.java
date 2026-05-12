package net.exohayvan.dissolver_enhanced.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntityType.Builder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;

public class ModBlockEntities {
    public static BlockEntityType<DissolverBlockEntity> DISSOLVER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "dissolver_block_entity",
            BlockEntityType.Builder.create(DissolverBlockEntity::new, ModBlocks.DISSOLVER_BLOCK)
    );

    public static BlockEntityType<CondenserBlockEntity> CONDENSER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "condenser_block_entity",
            BlockEntityType.Builder.create(CondenserBlockEntity::new, ModBlocks.CONDENSER_BLOCK)
    );

    public static BlockEntityType<MaterializerBlockEntity> MATERIALIZER_BLOCK_ENTITY = create(
            DissolverEnhanced.MOD_ID,
            "materializer_block_entity",
            BlockEntityType.Builder.create(MaterializerBlockEntity::new, ModBlocks.MATERIALIZER_BLOCK)
    );
    
    static <T extends BlockEntity> BlockEntityType<T> create(String namespace, String id, Builder<T> builder) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(namespace, id), builder.build(null));
    }

    public static void init() {
    }
}
