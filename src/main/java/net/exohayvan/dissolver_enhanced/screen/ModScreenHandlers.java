package net.exohayvan.dissolver_enhanced.screen;

import java.util.HashMap;
import java.util.UUID;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static HashMap<UUID, DissolverScreenHandler> activeHandlers = new HashMap<>();

    private static final MenuType<DissolverScreenHandler> DISSOLVER_SCREEN = new MenuType<>((syncId, playerInventory) -> new DissolverScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET);
    public static final MenuType<DissolverScreenHandler> DISSOLVER_SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, new ResourceLocation(DissolverEnhanced.MOD_ID, "dissolver_screen_handler"), DISSOLVER_SCREEN);

    public static void init() {
    }
}
