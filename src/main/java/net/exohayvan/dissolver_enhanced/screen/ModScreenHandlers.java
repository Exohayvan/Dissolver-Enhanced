package net.exohayvan.dissolver_enhanced.screen;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModScreenHandlers {
    public static HashMap<UUID, DissolverScreenHandler> activeHandlers = new HashMap<>();

    private static final ScreenHandlerType<DissolverScreenHandler> DISSOLVER_SCREEN = new ScreenHandlerType<>((syncId, playerInventory) -> new DissolverScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_FEATURES);
    public static final ScreenHandlerType<DissolverScreenHandler> DISSOLVER_SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(DissolverEnhanced.MOD_ID, "dissolver_screen_handler"), DISSOLVER_SCREEN);

    public static void init() {
    }
}
