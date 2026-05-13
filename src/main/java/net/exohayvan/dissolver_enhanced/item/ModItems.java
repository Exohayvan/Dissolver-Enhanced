package net.exohayvan.dissolver_enhanced.item;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item CRYSTAL_FRAME_ITEM = registerItem("crystal_frame_item", new CrystalFrameItem(itemProperties("crystal_frame_item")));
    public static final Item EMC_ORB = registerItem("emc_orb", new EMCOrbItem(itemProperties("emc_orb").stacksTo(1)));
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
		Identifier itemID = Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id);
		Item registeredItem = Registry.register(BuiltInRegistries.ITEM, itemID, item);

		return registeredItem;
    }

    public static Item.Properties itemProperties(String id) {
        return new Item.Properties().setId(itemKey(id));
    }

    private static Item registerEmcCore(String id, int emcPerSecond) {
        return registerItem(id, new EmcCoreItem(itemProperties(id).stacksTo(1), emcPerSecond));
    }

    private static ResourceKey<Item> itemKey(String id) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id));
    }

    // INITIALIZE

    public static void init() {
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
