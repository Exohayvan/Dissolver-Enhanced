package net.exohayvan.dissolver_enhanced.screen;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientScreenHandlers {
    public static void registerScreenHandlers(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.DISSOLVER_SCREEN_HANDLER_TYPE.get(), DissolverScreen::new);
        event.register(ModScreenHandlers.CONDENSER_SCREEN_HANDLER_TYPE.get(), CondenserScreen::new);
        event.register(ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE.get(), MaterializerScreen::new);
    }
}
