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

    private static final ScreenHandlerType<CondenserScreenHandler> CONDENSER_SCREEN = new ScreenHandlerType<>((syncId, playerInventory) -> new CondenserScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_FEATURES);
    public static final ScreenHandlerType<CondenserScreenHandler> CONDENSER_SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(DissolverEnhanced.MOD_ID, "condenser_screen_handler"), CONDENSER_SCREEN);

    private static final ScreenHandlerType<MaterializerScreenHandler> MATERIALIZER_SCREEN = new ScreenHandlerType<>((syncId, playerInventory) -> new MaterializerScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_FEATURES);
    public static final ScreenHandlerType<MaterializerScreenHandler> MATERIALIZER_SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(DissolverEnhanced.MOD_ID, "materializer_screen_handler"), MATERIALIZER_SCREEN);

    public static void init() {
    }
}
