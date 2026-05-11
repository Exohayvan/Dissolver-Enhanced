package net.exohayvan.dissolver_enhanced;

import net.fabricmc.api.ClientModInitializer;
import net.exohayvan.dissolver_enhanced.packets.DataReceiverClient;
import net.exohayvan.dissolver_enhanced.screen.ClientScreenHandlers;

public class DissolverEnhancedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DataReceiverClient.init();
		ClientScreenHandlers.registerScreenHandlers();
	}
}
