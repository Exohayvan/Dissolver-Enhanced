package net.exohayvan.dissolver_enhanced.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;

public class ModItemGroups {
    public static final CreativeModeTab VANILLAEMC_GROUP = registerItemGroup("dissolver_enhanced_group", "dissolver_enhanced.modname", ModItems.CRYSTAL_FRAME_ITEM, ModItemGroups::addToCustomInventory);

    private static void addToCustomInventory(ItemDisplayParameters displayContext, Output entries) {
        entries.accept(ModItems.CRYSTAL_FRAME_ITEM);
        entries.accept(ModItems.EMC_ORB);
        entries.accept(ModItems.COPPER_EMC_CORE);
        entries.accept(ModItems.IRON_EMC_CORE);
        entries.accept(ModItems.GOLD_EMC_CORE);
        entries.accept(ModItems.EMERALD_EMC_CORE);
        entries.accept(ModItems.DIAMOND_EMC_CORE);
        entries.accept(ModItems.NETHERITE_EMC_CORE);
        entries.accept(ModBlocks.DISSOLVER_BLOCK);
        entries.accept(ModBlocks.CONDENSER_BLOCK);
        entries.accept(ModBlocks.MATERIALIZER_BLOCK);
    }

    // HELPERS

	private static CreativeModeTab registerItemGroup(String id, String name, Item icon, CreativeModeTab.DisplayItemsGenerator entryList) {
		return Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable(name))
            .icon(() -> new ItemStack(icon))
            .displayItems(entryList)
            .build()
        );
    }

    // INITIALIZE

    public static void init() {
    }
}
