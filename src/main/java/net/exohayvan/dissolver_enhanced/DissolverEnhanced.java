package net.exohayvan.dissolver_enhanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValueUpdateMonitor;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.helpers.RecipeGenerator;
import net.exohayvan.dissolver_enhanced.item.ModItemGroups;
import net.exohayvan.dissolver_enhanced.item.ModItems;
import net.exohayvan.dissolver_enhanced.packets.Packets;
import net.exohayvan.dissolver_enhanced.particle.ModParticles;
import net.exohayvan.dissolver_enhanced.recipe.ModRecipeSerializers;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(DissolverEnhanced.MOD_ID)
public class DissolverEnhanced {
	public static final String MOD_ID = "dissolver_enhanced";
	public static final String OLD_MOD_ID = "vanillaemc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public DissolverEnhanced() {
		// Please use this code as inspiration for your projects! :)
		// For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.
		// John 3:16

		LOGGER.info("Initializing DissolverEnhanced!");
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ModItems.init(modEventBus);
		ModBlocks.init(modEventBus);
		ModItemGroups.init(modEventBus);
		ModBlockEntities.init(modEventBus);
		ModScreenHandlers.init(modEventBus);
		ModEntities.init(modEventBus);
		ModParticles.init(modEventBus);
		ModRecipeSerializers.init(modEventBus);

		ModConfig.init();
		DefaultEmcValueUpdateMonitor.start(
			ModConfig.defaultValuesFile(),
			LOGGER::info,
			(message, exception) -> LOGGER.warn(message, exception)
		);
		ModCriteria.init();
		RecipeGenerator.init();

		EMCValues.init();

		Packets.init();

		LOGGER.info("DissolverEnhanced ready!");
	}
}
