package net.exohayvan.dissolver_enhanced.packets.serverbound;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientPayload(String messageId, String data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClientPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "msg_to_server_payload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ClientPayload::messageId,
		ByteBufCodecs.STRING_UTF8, ClientPayload::data,
		ClientPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
