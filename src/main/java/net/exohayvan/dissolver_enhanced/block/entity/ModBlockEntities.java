package net.exohayvan.dissolver_enhanced.block.entity;

import java.lang.reflect.Constructor;
import java.util.Set;

import com.mojang.datafixers.types.Type;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DissolverBlockEntity>> DISSOLVER_BLOCK_ENTITY = register(
            "dissolver_block_entity",
            () -> create(DissolverBlockEntity::new, ModBlocks.DISSOLVER_BLOCK.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER_BLOCK_ENTITY = register(
            "condenser_block_entity",
            () -> create(CondenserBlockEntity::new, ModBlocks.CONDENSER_BLOCK.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaterializerBlockEntity>> MATERIALIZER_BLOCK_ENTITY = register(
            "materializer_block_entity",
            () -> create(MaterializerBlockEntity::new, ModBlocks.MATERIALIZER_BLOCK.get())
    );

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String id, java.util.function.Supplier<BlockEntityType<T>> supplier) {
        return BLOCK_ENTITIES.register(id, supplier);
    }

    public static void init(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    private static <T extends BlockEntity> BlockEntityType<T> create(
        BlockEntityType.BlockEntitySupplier<? extends T> factory,
        Block block
    ) {
        Set<Block> blocks = Set.of(block);
        BlockEntityType<T> withDataType = construct(factory, blocks, new Class<?>[] { Type.class }, new Object[] { null });
        if (withDataType != null) {
            return withDataType;
        }

        BlockEntityType<T> withoutDataType = construct(factory, blocks, new Class<?>[0], new Object[0]);
        if (withoutDataType != null) {
            return withoutDataType;
        }

        throw new IllegalStateException("Could not create block entity type for " + block);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityType<T> construct(
        BlockEntityType.BlockEntitySupplier<? extends T> factory,
        Set<Block> blocks,
        Class<?>[] extraParameterTypes,
        Object[] extraArguments
    ) {
        Class<?>[] parameterTypes = new Class<?>[extraParameterTypes.length + 2];
        parameterTypes[0] = BlockEntityType.BlockEntitySupplier.class;
        parameterTypes[1] = Set.class;
        System.arraycopy(extraParameterTypes, 0, parameterTypes, 2, extraParameterTypes.length);

        Object[] arguments = new Object[extraArguments.length + 2];
        arguments[0] = factory;
        arguments[1] = blocks;
        System.arraycopy(extraArguments, 0, arguments, 2, extraArguments.length);

        try {
            Constructor<BlockEntityType> constructor = BlockEntityType.class.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return (BlockEntityType<T>)constructor.newInstance(arguments);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
