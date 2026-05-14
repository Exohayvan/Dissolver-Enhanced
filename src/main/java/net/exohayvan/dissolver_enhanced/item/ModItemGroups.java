package net.exohayvan.dissolver_enhanced.item;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> ITEM_GROUPS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<CreativeModeTab> VANILLAEMC_GROUP = ITEM_GROUPS.register(
            "dissolver_enhanced_group",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("dissolver_enhanced.modname"))
                    .icon(() -> new ItemStack(ModItems.CRYSTAL_FRAME_ITEM.get()))
                    .displayItems(ModItemGroups::addToCustomInventory)
                    .build()
    );

    private static void addToCustomInventory(ItemDisplayParameters displayContext, Output entries) {
        entries.accept(ModItems.CRYSTAL_FRAME_ITEM.get());
        entries.accept(ModItems.EMC_ORB.get());
        entries.accept(ModItems.COPPER_EMC_CORE.get());
        entries.accept(ModItems.IRON_EMC_CORE.get());
        entries.accept(ModItems.GOLD_EMC_CORE.get());
        entries.accept(ModItems.EMERALD_EMC_CORE.get());
        entries.accept(ModItems.DIAMOND_EMC_CORE.get());
        entries.accept(ModItems.NETHERITE_EMC_CORE.get());
        entries.accept(ModBlocks.DISSOLVER_BLOCK.get());
        entries.accept(ModBlocks.CONDENSER_BLOCK.get());
        entries.accept(ModBlocks.MATERIALIZER_BLOCK.get());
    }

    public static void init(IEventBus eventBus) {
        ITEM_GROUPS.register(eventBus);
    }
}
