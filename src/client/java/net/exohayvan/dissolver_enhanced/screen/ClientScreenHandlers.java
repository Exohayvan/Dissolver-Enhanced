package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ClientScreenHandlers {
    public static void registerScreenHandlers() {
        HandledScreens.register(ModScreenHandlers.DISSOLVER_SCREEN_HANDLER_TYPE, DissolverScreen::new);
        HandledScreens.register(ModScreenHandlers.CONDENSER_SCREEN_HANDLER_TYPE, CondenserScreen::new);
        HandledScreens.register(ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE, MaterializerScreen::new);
    }
}
