package net.exohayvan.dissolver_enhanced.item;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<Item> CRYSTAL_FRAME_ITEM = ITEMS.register("crystal_frame_item", () -> new CrystalFrameItem(new Item.Properties()));
    public static final RegistryObject<Item> EMC_ORB = ITEMS.register("emc_orb", () -> new EMCOrbItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COPPER_EMC_CORE = registerEmcCore("copper_emc_core", 2);
    public static final RegistryObject<Item> IRON_EMC_CORE = registerEmcCore("iron_emc_core", 8);
    public static final RegistryObject<Item> GOLD_EMC_CORE = registerEmcCore("gold_emc_core", 16);
    public static final RegistryObject<Item> EMERALD_EMC_CORE = registerEmcCore("emerald_emc_core", 128);
    public static final RegistryObject<Item> DIAMOND_EMC_CORE = registerEmcCore("diamond_emc_core", 2048);
    public static final RegistryObject<Item> NETHERITE_EMC_CORE = registerEmcCore("netherite_emc_core", 262144);

    // private static void addToVanillaTools(FabricItemGroupEntries entries) {
    //     entries.add(MAGIC_ITEM);
    // }

    // HELPERS

    private static RegistryObject<Item> registerEmcCore(String id, int emcPerSecond) {
        return ITEMS.register(id, () -> new EmcCoreItem(new Item.Properties().stacksTo(1), emcPerSecond));
    }

    // INITIALIZE

    public static void init(IEventBus eventBus) {
        ITEMS.register(eventBus);
        // ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToVanillaTools);
    }
}
