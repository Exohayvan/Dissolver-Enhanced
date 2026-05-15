package net.exohayvan.dissolver_enhanced;

import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.analytics.ClientAnalytics;
import net.exohayvan.dissolver_enhanced.overlay.EMCOverlay;
import net.exohayvan.dissolver_enhanced.particle.ModParticles;
import net.exohayvan.dissolver_enhanced.render.CrystalEntityRenderer;
import net.exohayvan.dissolver_enhanced.screen.ClientScreenHandlers;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = DissolverEnhanced.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DissolverEnhancedClient {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISSOLVER_BLOCK.get(), RenderType.translucent());
			EMCOverlay.init();
			ClientAnalytics.init();
		});
	}

	@SubscribeEvent
	public static void registerMenuScreens(RegisterMenuScreensEvent event) {
		ClientScreenHandlers.registerScreenHandlers(event);
	}

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.CRYSTAL_ENTITY.get(), CrystalEntityRenderer::new);
	}

	@SubscribeEvent
	public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ModParticles.CRYSTAL.get(), EndRodParticle.Provider::new);
	}
}
