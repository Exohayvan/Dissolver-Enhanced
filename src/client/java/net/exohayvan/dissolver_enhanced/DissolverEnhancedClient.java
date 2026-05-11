package net.exohayvan.dissolver_enhanced;

import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.overlay.EMCOverlay;
import net.exohayvan.dissolver_enhanced.particle.ModParticles;
import net.exohayvan.dissolver_enhanced.render.CrystalEntityRenderer;
import net.exohayvan.dissolver_enhanced.screen.ClientScreenHandlers;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = DissolverEnhanced.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DissolverEnhancedClient {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ClientScreenHandlers.registerScreenHandlers();
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISSOLVER_BLOCK, RenderType.translucent());
			EMCOverlay.init();
		});
	}

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.CRYSTAL_ENTITY, CrystalEntityRenderer::new);
	}

	@SubscribeEvent
	public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ModParticles.CRYSTAL, EndRodParticle.Provider::new);
	}
}
