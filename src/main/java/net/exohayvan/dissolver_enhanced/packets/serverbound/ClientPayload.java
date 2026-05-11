package net.exohayvan.dissolver_enhanced.packets.serverbound;

import java.util.function.Supplier;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public record ClientPayload(String messageId, String data) {
	public static void encode(ClientPayload payload, FriendlyByteBuf buffer) {
		buffer.writeUtf(payload.messageId());
		buffer.writeUtf(payload.data());
	}

	public static ClientPayload decode(FriendlyByteBuf buffer) {
		return new ClientPayload(buffer.readUtf(), buffer.readUtf());
	}

	public static void handle(ClientPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> receivedData(context.getSender(), payload.messageId(), payload.data()));
		context.setPacketHandled(true);
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
