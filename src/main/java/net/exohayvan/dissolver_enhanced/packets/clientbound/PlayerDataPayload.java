package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public record PlayerDataPayload(int emc, int learnedItemsSize, int learnedItemsTotalSize, String message, List<String> learnedItems) implements CustomPayload {
	public static final CustomPayload.Id<PlayerDataPayload> ID = new CustomPayload.Id<>(Identifier.of(DissolverEnhanced.MOD_ID, "playerdata_to_client_payload"));
	public static final PacketCodec<RegistryByteBuf, PlayerDataPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.INTEGER, PlayerDataPayload::emc,
        PacketCodecs.INTEGER, PlayerDataPayload::learnedItemsSize,
        PacketCodecs.INTEGER, PlayerDataPayload::learnedItemsTotalSize,
        PacketCodecs.STRING, PlayerDataPayload::message,
        PacketCodecs.STRING.collect(PacketCodecs.toCollection(ArrayList::new)), PlayerDataPayload::learnedItems,
		PlayerDataPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
