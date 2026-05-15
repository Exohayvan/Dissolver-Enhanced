package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DissolverBlockEntity>> DISSOLVER_BLOCK_ENTITY = register(
            "dissolver_block_entity",
            () -> BlockEntityType.Builder.of(DissolverBlockEntity::new, ModBlocks.DISSOLVER_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER_BLOCK_ENTITY = register(
            "condenser_block_entity",
            () -> BlockEntityType.Builder.of(CondenserBlockEntity::new, ModBlocks.CONDENSER_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaterializerBlockEntity>> MATERIALIZER_BLOCK_ENTITY = register(
            "materializer_block_entity",
            () -> BlockEntityType.Builder.of(MaterializerBlockEntity::new, ModBlocks.MATERIALIZER_BLOCK.get()).build(null)
    );

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String id, java.util.function.Supplier<BlockEntityType<T>> supplier) {
        return BLOCK_ENTITIES.register(id, supplier);
    }

    public static void init(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
