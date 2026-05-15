package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EMCValuesPayload(int version, List<String> values) implements CustomPacketPayload {
	public static final Type<EMCValuesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "emc_values"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EMCValuesPayload> STREAM_CODEC = CustomPacketPayload.codec(EMCValuesPayload::encode, EMCValuesPayload::decode);

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(version());
		buffer.writeCollection(values(), FriendlyByteBuf::writeUtf);
	}

	public static EMCValuesPayload decode(FriendlyByteBuf buffer) {
		return new EMCValuesPayload(buffer.readInt(), buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf));
	}

	public static void handle(EMCValuesPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> EMCValues.applyClientSyncValues(payload.values()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
