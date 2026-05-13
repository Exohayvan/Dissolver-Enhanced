package net.exohayvan.dissolver_enhanced.block;

import java.util.function.ToIntFunction;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.item.DissolverBlockItem;
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

public class ModBlocks {
    private static final Block _DISSOLVER_BLOCK = createDissolverBlock();
    public static final DissolverBlockItem DISSOLVER_BLOCK_ITEM = new DissolverBlockItem(_DISSOLVER_BLOCK, new Item.Settings().rarity(Rarity.RARE));
    public static final Block DISSOLVER_BLOCK = registerBlock("dissolver_block", _DISSOLVER_BLOCK, DISSOLVER_BLOCK_ITEM);

    private static final Block _CONDENSER_BLOCK = createCondenserBlock();
    public static final Block CONDENSER_BLOCK = registerBlock("condenser_block", _CONDENSER_BLOCK, new BlockItem(_CONDENSER_BLOCK, new Item.Settings().rarity(Rarity.UNCOMMON)));

    private static final Block _MATERIALIZER_BLOCK = createMaterializerBlock();
    public static final Block MATERIALIZER_BLOCK = registerBlock("materializer_block", _MATERIALIZER_BLOCK, new BlockItem(_MATERIALIZER_BLOCK, new Item.Settings().rarity(Rarity.UNCOMMON)));

    private static Block createDissolverBlock() {
        return new DissolverBlock(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK).sounds(BlockSoundGroup.AMETHYST_BLOCK).luminance(getLuminance(12)).nonOpaque());
    }

    private static Block createCondenserBlock() {
        return new CondenserBlock(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK).sounds(BlockSoundGroup.AMETHYST_BLOCK).luminance(getLuminance(8)).nonOpaque());
    }

    private static Block createMaterializerBlock() {
        return new MaterializerBlock(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK).sounds(BlockSoundGroup.AMETHYST_BLOCK).luminance(getLuminance(8)).nonOpaque());
    }

    private static Block registerBlock(String id, Block block, BlockItem blockItem) {
        registerBlockItem(DissolverEnhanced.MOD_ID, id, blockItem);
        return Registry.register(Registries.BLOCK, Identifier.of(DissolverEnhanced.MOD_ID, id), block);
    }

    private static Item registerBlockItem(String namespace, String id, BlockItem blockItem) {
        return Registry.register(Registries.ITEM, Identifier.of(namespace, id), blockItem);
    }

    private static ToIntFunction<BlockState> getLuminance(int luminance) {
        return value -> luminance;
    }

    public static void init() {
    }
}
