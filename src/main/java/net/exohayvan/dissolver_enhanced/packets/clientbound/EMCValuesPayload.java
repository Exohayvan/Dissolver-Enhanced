package net.exohayvan.dissolver_enhanced.packets.clientbound;

import java.util.ArrayList;
import java.util.List;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EMCValuesPayload(int version, List<String> values) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EMCValuesPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "emc_values_to_client_payload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EMCValuesPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, EMCValuesPayload::version,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.collection(ArrayList::new)), EMCValuesPayload::values,
		EMCValuesPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
