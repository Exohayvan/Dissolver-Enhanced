package net.exohayvan.dissolver_enhanced.packets;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Packets {
	private static final String PROTOCOL_VERSION = "1";

	public static void init() {
		DissolverEnhanced.LOGGER.info("Registering packet payloads.");
	}

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(DissolverEnhanced.MOD_ID).versioned(PROTOCOL_VERSION);
		registrar.playToClient(EMCValuesPayload.TYPE, EMCValuesPayload.STREAM_CODEC, EMCValuesPayload::handle);
		registrar.playToClient(PlayerDataPayload.TYPE, PlayerDataPayload.STREAM_CODEC, PlayerDataPayload::handle);
		registrar.playToServer(ClientPayload.TYPE, ClientPayload.STREAM_CODEC, ClientPayload::handle);
	}
}
