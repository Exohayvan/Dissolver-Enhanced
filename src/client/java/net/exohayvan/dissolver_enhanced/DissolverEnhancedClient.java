package net.exohayvan.dissolver_enhanced;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.packets.DataReceiverClient;
import net.exohayvan.dissolver_enhanced.render.CrystalEntityRenderer;
import net.exohayvan.dissolver_enhanced.screen.ClientScreenHandlers;

public class DissolverEnhancedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DataReceiverClient.init();
		ClientScreenHandlers.registerScreenHandlers();
		EntityRendererRegistry.register(ModEntities.CRYSTAL_ENTITY, CrystalEntityRenderer::new);
	}
}
