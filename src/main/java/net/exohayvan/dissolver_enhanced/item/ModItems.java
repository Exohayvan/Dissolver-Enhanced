package net.exohayvan.dissolver_enhanced.item;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item CRYSTAL_FRAME_ITEM = registerItem("crystal_frame_item", new CrystalFrameItem(new Item.Properties()));

    // private static void addToVanillaTools(FabricItemGroupEntries entries) {
    //     entries.add(MAGIC_ITEM);
    // }

    // HELPERS

	private static Item registerItem(String id, Item item) {
		Identifier itemID = Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id);
		Item registeredItem = Registry.register(BuiltInRegistries.ITEM, itemID, item);

		return registeredItem;
    }

    // INITIALIZE

    public static void init() {
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
