package net.exohayvan.dissolver_enhanced.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroup.DisplayContext;
import net.minecraft.item.ItemGroup.Entries;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;

public class ModItemGroups {
    public static final ItemGroup VANILLAEMC_GROUP = registerItemGroup("dissolver_enhanced_group", "dissolver_enhanced.modname", ModItems.CRYSTAL_FRAME_ITEM, ModItemGroups::addToCustomInventory);

    private static void addToCustomInventory(DisplayContext displayContext, Entries entries) {
        entries.add(ModItems.CRYSTAL_FRAME_ITEM);
        entries.add(ModItems.EMC_ORB);
        entries.add(ModBlocks.DISSOLVER_BLOCK);
        entries.add(ModBlocks.CONDENSER_BLOCK);
        entries.add(ModBlocks.MATERIALIZER_BLOCK);
    }

    // HELPERS

	private static ItemGroup registerItemGroup(String id, String name, Item icon, ItemGroup.EntryCollector entryList) {
		return Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(DissolverEnhanced.MOD_ID, id),
            FabricItemGroup.builder()
            .displayName(Text.translatable(name))
            .icon(() -> new ItemStack(icon))
            .entries(entryList)
            .build()
        );
    }

    // INITIALIZE

    public static void init() {
    }
}
