package net.exohayvan.dissolver_enhanced.packets;

import java.util.Optional;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Packets {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(DissolverEnhanced.MOD_ID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);

	public static void init() {
		DissolverEnhanced.LOGGER.info("Registering packet payloads.");
		int id = 0;
		CHANNEL.registerMessage(id++, EMCValuesPayload.class, EMCValuesPayload::encode, EMCValuesPayload::decode, EMCValuesPayload::handle, Optional.empty());
		CHANNEL.registerMessage(id++, PlayerDataPayload.class, PlayerDataPayload::encode, PlayerDataPayload::decode, PlayerDataPayload::handle, Optional.empty());
		CHANNEL.registerMessage(id++, ClientPayload.class, ClientPayload::encode, ClientPayload::decode, ClientPayload::handle, Optional.empty());
	}
}
