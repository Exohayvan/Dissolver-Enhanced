package net.exohayvan.dissolver_enhanced.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModItems {
    public static final Item CRYSTAL_FRAME_ITEM = registerItem("crystal_frame_item", new CrystalFrameItem(new Item.Settings()));
    public static final Item EMC_ORB = registerItem("emc_orb", new EMCOrbItem(new Item.Settings().maxCount(1)));

    // private static void addToVanillaTools(FabricItemGroupEntries entries) {
    //     entries.add(MAGIC_ITEM);
    // }

    // HELPERS

	private static Item registerItem(String id, Item item) {
		Identifier itemID = Identifier.of(DissolverEnhanced.MOD_ID, id);
		Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

		return registeredItem;
    }

    // INITIALIZE

    public static void init() {
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
