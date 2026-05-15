package net.exohayvan.dissolver_enhanced.item;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<Item, Item> CRYSTAL_FRAME_ITEM = ITEMS.register("crystal_frame_item", () -> new CrystalFrameItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> EMC_ORB = ITEMS.register("emc_orb", () -> new EMCOrbItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> COPPER_EMC_CORE = registerEmcCore("copper_emc_core", 2);
    public static final DeferredHolder<Item, Item> IRON_EMC_CORE = registerEmcCore("iron_emc_core", 8);
    public static final DeferredHolder<Item, Item> GOLD_EMC_CORE = registerEmcCore("gold_emc_core", 16);
    public static final DeferredHolder<Item, Item> EMERALD_EMC_CORE = registerEmcCore("emerald_emc_core", 128);
    public static final DeferredHolder<Item, Item> DIAMOND_EMC_CORE = registerEmcCore("diamond_emc_core", 2048);
    public static final DeferredHolder<Item, Item> NETHERITE_EMC_CORE = registerEmcCore("netherite_emc_core", 262144);

    // private static void addToVanillaTools(FabricItemGroupEntries entries) {
    //     entries.add(MAGIC_ITEM);
    // }

    // HELPERS

    private static DeferredHolder<Item, Item> registerEmcCore(String id, int emcPerSecond) {
        return ITEMS.register(id, () -> new EmcCoreItem(new Item.Properties().stacksTo(1), emcPerSecond));
    }

    // INITIALIZE

    public static void init(IEventBus eventBus) {
        ITEMS.register(eventBus);
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
