package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.screens.MenuScreens;

public class ClientScreenHandlers {
    public static void registerScreenHandlers() {
        MenuScreens.register(ModScreenHandlers.DISSOLVER_SCREEN_HANDLER_TYPE.get(), DissolverScreen::new);
        MenuScreens.register(ModScreenHandlers.CONDENSER_SCREEN_HANDLER_TYPE.get(), CondenserScreen::new);
        MenuScreens.register(ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE.get(), MaterializerScreen::new);
    }
}
