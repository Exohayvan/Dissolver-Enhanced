package net.exohayvan.dissolver_enhanced.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.helpers.RegistryKeyCompat;

public class ModItems {
    public static final Item CRYSTAL_FRAME_ITEM = registerItem("crystal_frame_item", new CrystalFrameItem(RegistryKeyCompat.itemSettings("crystal_frame_item")));
    public static final Item EMC_ORB = registerItem("emc_orb", new EMCOrbItem(RegistryKeyCompat.itemSettings("emc_orb").maxCount(1)));
    public static final Item COPPER_EMC_CORE = registerEmcCore("copper_emc_core", 2);
    public static final Item IRON_EMC_CORE = registerEmcCore("iron_emc_core", 8);
    public static final Item GOLD_EMC_CORE = registerEmcCore("gold_emc_core", 16);
    public static final Item EMERALD_EMC_CORE = registerEmcCore("emerald_emc_core", 128);
    public static final Item DIAMOND_EMC_CORE = registerEmcCore("diamond_emc_core", 2048);
    public static final Item NETHERITE_EMC_CORE = registerEmcCore("netherite_emc_core", 262144);

    // private static void addToVanillaTools(FabricItemGroupEntries entries) {
    //     entries.add(MAGIC_ITEM);
    // }

    // HELPERS

	private static Item registerItem(String id, Item item) {
		Identifier itemID = Identifier.of(DissolverEnhanced.MOD_ID, id);
		Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

		return registeredItem;
    }

    private static Item registerEmcCore(String id, int emcPerSecond) {
        return registerItem(id, new EmcCoreItem(RegistryKeyCompat.itemSettings(id).maxCount(1), emcPerSecond));
    }

    // INITIALIZE

    public static void init() {
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
