package net.exohayvan.dissolver_enhanced;

import java.lang.reflect.Method;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.render.RenderLayer;
import net.exohayvan.dissolver_enhanced.analytics.ClientAnalytics;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.helpers.MinecraftVersionCompat;
import net.exohayvan.dissolver_enhanced.overlay.EMCOverlay;
import net.exohayvan.dissolver_enhanced.packets.DataReceiverClient;
import net.exohayvan.dissolver_enhanced.particle.ModParticles;
import net.exohayvan.dissolver_enhanced.render.CrystalEntityRenderer;
import net.exohayvan.dissolver_enhanced.screen.ClientScreenHandlers;

public class DissolverEnhancedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DataReceiverClient.init();
		ClientAnalytics.init();
		
        ClientScreenHandlers.registerScreenHandlers();

		// transparent
		registerBlockRenderLayer();

		if (MinecraftVersionCompat.isLegacyRendererVersion()) {
			EntityRendererRegistry.register(ModEntities.CRYSTAL_ENTITY, CrystalEntityRenderer::new);
		}

		// particle
		ParticleFactoryRegistry.getInstance().register(ModParticles.CRYSTAL, EndRodParticle.Factory::new);

		EMCOverlay.init();
	}

	private static void registerBlockRenderLayer() {
		try {
			Class<?> mapClass = Class.forName("net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap");
			Object map = mapClass.getField("INSTANCE").get(null);

			for (Method method : mapClass.getMethods()) {
				if (!method.getName().equals("putBlock") || method.getParameterCount() != 2) {
					continue;
				}

				method.invoke(map, ModBlocks.DISSOLVER_BLOCK, RenderLayer.getTranslucent());
				return;
			}
		} catch (ClassNotFoundException exception) {
			DissolverEnhanced.LOGGER.debug("Fabric block render layer API is unavailable; skipping dissolver block render layer registration.");
		} catch (ReflectiveOperationException exception) {
			DissolverEnhanced.LOGGER.warn("Unable to register dissolver block render layer.", exception);
		}
	}
}
