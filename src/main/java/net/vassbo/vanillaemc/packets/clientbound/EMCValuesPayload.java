package net.vassbo.vanillaemc.packets.clientbound;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.vassbo.vanillaemc.VanillaEMC;

public record EMCValuesPayload(int version, List<String> values) implements CustomPayload {
	public static final CustomPayload.Id<EMCValuesPayload> ID = new CustomPayload.Id<>(Identifier.of(VanillaEMC.MOD_ID, "emc_values_to_client_payload"));
	public static final PacketCodec<RegistryByteBuf, EMCValuesPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.INTEGER, EMCValuesPayload::version,
		PacketCodecs.STRING.collect(PacketCodecs.toCollection(ArrayList::new)), EMCValuesPayload::values,
		EMCValuesPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
