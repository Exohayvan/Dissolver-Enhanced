package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PlayerDataPayload(int emc, int learnedItemsSize, int learnedItemsTotalSize, String message, List<String> learnedItems) {
	public static void encode(PlayerDataPayload payload, FriendlyByteBuf buffer) {
		buffer.writeInt(payload.emc());
		buffer.writeInt(payload.learnedItemsSize());
		buffer.writeInt(payload.learnedItemsTotalSize());
		buffer.writeUtf(payload.message());
		buffer.writeCollection(payload.learnedItems(), FriendlyByteBuf::writeUtf);
	}

	public static PlayerDataPayload decode(FriendlyByteBuf buffer) {
		return new PlayerDataPayload(
			buffer.readInt(),
			buffer.readInt(),
			buffer.readInt(),
			buffer.readUtf(),
			buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
		);
	}

	public static void handle(PlayerDataPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			PlayerDataClient.EMC = payload.emc();
			PlayerDataClient.LEARNED_ITEMS_SIZE = payload.learnedItemsSize();
			PlayerDataClient.LEARNED_ITEMS_TOTAL_SIZE = payload.learnedItemsTotalSize();
			PlayerDataClient.MESSAGE = payload.message();
			PlayerDataClient.LEARNED_ITEMS = payload.learnedItems();
		});
		context.setPacketHandled(true);
	}
}
