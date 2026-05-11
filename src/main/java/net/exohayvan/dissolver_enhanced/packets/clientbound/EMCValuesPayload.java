package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record EMCValuesPayload(int version, List<String> values) {
	public static void encode(EMCValuesPayload payload, FriendlyByteBuf buffer) {
		buffer.writeInt(payload.version());
		buffer.writeCollection(payload.values(), FriendlyByteBuf::writeUtf);
	}

	public static EMCValuesPayload decode(FriendlyByteBuf buffer) {
		return new EMCValuesPayload(buffer.readInt(), buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf));
	}

	public static void handle(EMCValuesPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> EMCValues.applyClientSyncValues(payload.values()));
		context.setPacketHandled(true);
	}
}
