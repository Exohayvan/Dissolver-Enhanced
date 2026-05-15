package net.exohayvan.dissolver_enhanced.packets.serverbound;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientPayload(String messageId, String data) implements CustomPacketPayload {
	public static final Type<ClientPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "client"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientPayload> STREAM_CODEC = CustomPacketPayload.codec(ClientPayload::encode, ClientPayload::decode);

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(messageId());
		buffer.writeUtf(data());
	}

	public static ClientPayload decode(FriendlyByteBuf buffer) {
		return new ClientPayload(buffer.readUtf(), buffer.readUtf());
	}

	public static void handle(ClientPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> receivedData(context.player(), payload.messageId(), payload.data()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
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
