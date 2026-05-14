package net.exohayvan.dissolver_enhanced.screen;

import java.util.HashMap;
import java.util.UUID;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModScreenHandlers {
    public static HashMap<UUID, DissolverScreenHandler> activeHandlers = new HashMap<>();
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<MenuType<DissolverScreenHandler>> DISSOLVER_SCREEN_HANDLER_TYPE = MENU_TYPES.register(
            "dissolver_screen_handler",
            () -> new MenuType<>((syncId, playerInventory) -> new DissolverScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET)
    );

    public static final RegistryObject<MenuType<CondenserScreenHandler>> CONDENSER_SCREEN_HANDLER_TYPE = MENU_TYPES.register(
            "condenser_screen_handler",
            () -> new MenuType<>((syncId, playerInventory) -> new CondenserScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET)
    );

    public static final RegistryObject<MenuType<MaterializerScreenHandler>> MATERIALIZER_SCREEN_HANDLER_TYPE = MENU_TYPES.register(
            "materializer_screen_handler",
            () -> new MenuType<>((syncId, playerInventory) -> new MaterializerScreenHandler(syncId, playerInventory), FeatureFlags.VANILLA_SET)
    );

    public static void init(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
