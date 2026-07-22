package net.exohayvan.dissolver_enhanced.packets.serverbound;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.NetworkCompat;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientPayload(String messageId, String data) {
	public static void encode(ClientPayload payload, FriendlyByteBuf buffer) {
		buffer.writeUtf(payload.messageId());
		buffer.writeUtf(payload.data());
	}

	public static ClientPayload decode(FriendlyByteBuf buffer) {
		return new ClientPayload(buffer.readUtf(), buffer.readUtf());
	}

	public static void handle(ClientPayload payload, Object context) {
		NetworkCompat.enqueueWork(context, () -> receivedData(NetworkCompat.sender(context), payload.messageId(), payload.data()));
		NetworkCompat.setPacketHandled(context);
	}

	private static void receivedData(Player player, String messageId, String data) {
		if (player == null) return;

		if (messageId.contains("search")) {
			ModScreenHandlers.activeHandlers.get(player.getUUID()).search(data);
		} else if (messageId.contains("scroll")) {
			ModScreenHandlers.activeHandlers.get(player.getUUID()).scrollItems(Float.parseFloat(data));
		} else {
			DissolverEnhanced.LOGGER.info("RECEIVED MESSAGE FROM CLIENT: " + messageId);
		}
	}
}
