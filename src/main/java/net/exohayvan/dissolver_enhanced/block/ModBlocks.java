package net.exohayvan.dissolver_enhanced.block;

import java.util.function.ToIntFunction;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.item.DissolverBlockItem;
import net.exohayvan.dissolver_enhanced.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ModBlocks {
    // DISSOLVER
    private static final Block _DISSOLVER_BLOCK = createDissolverBlock("dissolver_block");
    public static final DissolverBlockItem DISSOLVER_BLOCK_ITEM = new DissolverBlockItem(_DISSOLVER_BLOCK, ModItems.itemProperties("dissolver_block").rarity(Rarity.RARE));
    public static final Block DISSOLVER_BLOCK = registerBlock("dissolver_block", _DISSOLVER_BLOCK, DISSOLVER_BLOCK_ITEM);
    
    // HELPERS

    private static Block createDissolverBlock(String id) {
        return new DissolverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).setId(blockKey(id)).sound(SoundType.AMETHYST).lightLevel(getLuminance(12)).noOcclusion());
    }
    
    private static Block registerBlock(String id, Block block, BlockItem blockItem) {
        registerBlockItem(DissolverEnhanced.MOD_ID, id, blockItem);
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id), block);
    }

    private static Item registerBlockItem(String namespace, String id, BlockItem blockItem) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(namespace, id), blockItem);
    }

    private static ResourceKey<Block> blockKey(String id) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id));
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

    public static void init() {
    }
}
