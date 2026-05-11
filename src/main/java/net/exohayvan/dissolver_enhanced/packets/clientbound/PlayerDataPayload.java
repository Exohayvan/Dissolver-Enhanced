package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerDataPayload(int emc, int learnedItemsSize, int learnedItemsTotalSize, String message, List<String> learnedItems) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerDataPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "playerdata_to_client_payload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerDataPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, PlayerDataPayload::emc,
        ByteBufCodecs.INT, PlayerDataPayload::learnedItemsSize,
        ByteBufCodecs.INT, PlayerDataPayload::learnedItemsTotalSize,
        ByteBufCodecs.STRING_UTF8, PlayerDataPayload::message,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.collection(ArrayList::new)), PlayerDataPayload::learnedItems,
		PlayerDataPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
