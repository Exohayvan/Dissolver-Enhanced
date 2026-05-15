package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerDataPayload(String emc, int learnedItemsSize, int learnedItemsTotalSize, String message, List<String> learnedItems) implements CustomPacketPayload {
	public static final Type<PlayerDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "player_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerDataPayload> STREAM_CODEC = CustomPacketPayload.codec(PlayerDataPayload::encode, PlayerDataPayload::decode);

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(emc());
		buffer.writeInt(learnedItemsSize());
		buffer.writeInt(learnedItemsTotalSize());
		buffer.writeUtf(message());
		buffer.writeCollection(learnedItems(), FriendlyByteBuf::writeUtf);
	}

	public static PlayerDataPayload decode(FriendlyByteBuf buffer) {
		return new PlayerDataPayload(
			buffer.readUtf(),
			buffer.readInt(),
			buffer.readInt(),
			buffer.readUtf(),
			buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
		);
	}

	public static void handle(PlayerDataPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			PlayerDataClient.EMC = net.exohayvan.dissolver_enhanced.common.values.EmcNumber.parse(payload.emc());
			PlayerDataClient.LEARNED_ITEMS_SIZE = payload.learnedItemsSize();
			PlayerDataClient.LEARNED_ITEMS_TOTAL_SIZE = payload.learnedItemsTotalSize();
			PlayerDataClient.MESSAGE = payload.message();
			PlayerDataClient.LEARNED_ITEMS = payload.learnedItems();
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
