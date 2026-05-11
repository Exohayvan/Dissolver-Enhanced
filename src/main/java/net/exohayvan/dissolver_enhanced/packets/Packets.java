package net.exohayvan.dissolver_enhanced.packets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;

public class Packets {
	public static void init() {
        DissolverEnhanced.LOGGER.info("Registering packet payloads.");
		clientbound(PayloadTypeRegistry.clientboundPlay());
		serverbound(PayloadTypeRegistry.serverboundPlay());
	}

	private static void clientbound(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
		registry.register(EMCValuesPayload.ID, EMCValuesPayload.CODEC);
		registry.register(PlayerDataPayload.ID, PlayerDataPayload.CODEC);
	}

	private static void serverbound(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry) {
		registry.register(ClientPayload.ID, ClientPayload.CODEC);
	}
}
