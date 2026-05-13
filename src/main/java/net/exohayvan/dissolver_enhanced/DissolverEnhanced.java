package net.exohayvan.dissolver_enhanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.block.entity.ModBlockEntities;
import net.exohayvan.dissolver_enhanced.command.ModCommands;
import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValueUpdateMonitor;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.entity.ModEntities;
import net.exohayvan.dissolver_enhanced.event.JoinEvent;
import net.exohayvan.dissolver_enhanced.helpers.RecipeGenerator;
import net.exohayvan.dissolver_enhanced.item.ModItemGroups;
import net.exohayvan.dissolver_enhanced.item.ModItems;
import net.exohayvan.dissolver_enhanced.packets.DataReceiver;
import net.exohayvan.dissolver_enhanced.packets.Packets;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;

public class DissolverEnhanced implements ModInitializer {
	public static final String MOD_ID = "dissolver_enhanced";
	public static final String OLD_MOD_ID = "vanillaemc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Please use this code as inspiration for your projects! :)
		// For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.
		// John 3:16

		LOGGER.info("Initializing DissolverEnhanced!");

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
		DataReceiver.init();

        JoinEvent.init();
		
		ModCommands.init();

		ModItemGroups.init();
		ModItems.init();
		ModBlocks.init();

		// ModParticles.init();
		ModEntities.init();

		ModBlockEntities.init();
		ModScreenHandlers.init();

		LOGGER.info("DissolverEnhanced ready!");
	}
}
