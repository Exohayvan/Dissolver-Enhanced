package net.exohayvan.dissolver_enhanced.block;

import java.util.function.ToIntFunction;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.item.DissolverBlockItem;

public class ModBlocks {
    // DISSOLVER
    private static final Block _DISSOLVER_BLOCK = createDissolverBlock();
    public static final DissolverBlockItem DISSOLVER_BLOCK_ITEM = new DissolverBlockItem(_DISSOLVER_BLOCK, new Item.Settings().rarity(Rarity.RARE));
    public static final Block DISSOLVER_BLOCK = registerBlock("dissolver_block", _DISSOLVER_BLOCK, DISSOLVER_BLOCK_ITEM);
    
    // HELPERS

    private static Block createDissolverBlock() {
        return new DissolverBlock(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK).sounds(BlockSoundGroup.AMETHYST_BLOCK).luminance(getLuminance(12)).nonOpaque());
    }
    
    private static Block registerBlock(String id, Block block, BlockItem blockItem) {
        registerBlockItem(DissolverEnhanced.MOD_ID, id, blockItem);
        return Registry.register(Registries.BLOCK, Identifier.of(DissolverEnhanced.MOD_ID, id), block);
    }

    private static Item registerBlockItem(String namespace, String id, BlockItem blockItem) {
        return Registry.register(Registries.ITEM, Identifier.of(namespace, id), blockItem);
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
