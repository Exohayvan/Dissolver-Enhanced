package net.exohayvan.dissolver_enhanced.screen;

import java.util.HashMap;
import java.util.UUID;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static HashMap<UUID, DissolverScreenHandler> activeHandlers = new HashMap<>();

    private static final MenuType<DissolverScreenHandler> DISSOLVER_SCREEN = new MenuType<>((syncId, playerInventory) -> new DissolverScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET);
    public static final MenuType<DissolverScreenHandler> DISSOLVER_SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "dissolver_screen_handler"), DISSOLVER_SCREEN);

    private static final MenuType<CondenserScreenHandler> CONDENSER_SCREEN = new MenuType<>((syncId, playerInventory) -> new CondenserScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET);
    public static final MenuType<CondenserScreenHandler> CONDENSER_SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "condenser_screen_handler"), CONDENSER_SCREEN);

    private static final MenuType<MaterializerScreenHandler> MATERIALIZER_SCREEN = new MenuType<>((syncId, playerInventory) -> new MaterializerScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET);
    public static final MenuType<MaterializerScreenHandler> MATERIALIZER_SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "materializer_screen_handler"), MATERIALIZER_SCREEN);

    public static void init() {
    }
}
