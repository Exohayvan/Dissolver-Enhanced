package net.exohayvan.dissolver_enhanced.block;

import java.util.function.ToIntFunction;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.item.DissolverBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DissolverEnhanced.MOD_ID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<Block> DISSOLVER_BLOCK = BLOCKS.register("dissolver_block", ModBlocks::createDissolverBlock);
    public static final RegistryObject<Item> DISSOLVER_BLOCK_ITEM = BLOCK_ITEMS.register("dissolver_block", () -> new DissolverBlockItem(DISSOLVER_BLOCK.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Block> CONDENSER_BLOCK = BLOCKS.register("condenser_block", ModBlocks::createCondenserBlock);
    public static final RegistryObject<Item> CONDENSER_BLOCK_ITEM = BLOCK_ITEMS.register("condenser_block", () -> new BlockItem(CONDENSER_BLOCK.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Block> MATERIALIZER_BLOCK = BLOCKS.register("materializer_block", ModBlocks::createMaterializerBlock);
    public static final RegistryObject<Item> MATERIALIZER_BLOCK_ITEM = BLOCK_ITEMS.register("materializer_block", () -> new BlockItem(MATERIALIZER_BLOCK.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    
    // HELPERS

    private static Block createDissolverBlock() {
        return new DissolverBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.AMETHYST).lightLevel(getLuminance(12)).noOcclusion());
    }

    private static Block createCondenserBlock() {
        return new CondenserBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.AMETHYST).lightLevel(getLuminance(8)).noOcclusion());
    }

    private static Block createMaterializerBlock() {
        return new MaterializerBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.AMETHYST).lightLevel(getLuminance(8)).noOcclusion());
    }
    
    private static ToIntFunction<BlockState> getLuminance(int luminance) {
        return new ToIntFunction<BlockState>() {
            @Override
            public int applyAsInt(BlockState value) {
                return luminance;
            }
        };
    }

    // INITIALIZE

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
