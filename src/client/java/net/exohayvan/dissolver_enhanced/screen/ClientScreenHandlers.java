package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.screens.MenuScreens;

public class ClientScreenHandlers {
    public static void registerScreenHandlers() {
        MenuScreens.register(ModScreenHandlers.DISSOLVER_SCREEN_HANDLER_TYPE, DissolverScreen::new);
    }
}