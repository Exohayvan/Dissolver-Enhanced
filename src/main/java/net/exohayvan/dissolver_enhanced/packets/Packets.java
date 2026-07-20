package net.exohayvan.dissolver_enhanced.packets;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class Packets {
    private static final Object CHANNEL = NetworkCompat.createChannel(
        new ResourceLocation(DissolverEnhanced.MOD_ID, "main")
    );

    public static void init() {
        DissolverEnhanced.LOGGER.info("Registering packet payloads.");
        int id = 0;
        NetworkCompat.register(CHANNEL, id++, EMCValuesPayload.class, EMCValuesPayload::encode, EMCValuesPayload::decode, EMCValuesPayload::handle, "PLAY_TO_CLIENT");
        NetworkCompat.register(CHANNEL, id++, PlayerDataPayload.class, PlayerDataPayload::encode, PlayerDataPayload::decode, PlayerDataPayload::handle, "PLAY_TO_CLIENT");
        NetworkCompat.register(CHANNEL, id++, ClientPayload.class, ClientPayload::encode, ClientPayload::decode, ClientPayload::handle, "PLAY_TO_SERVER");
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        NetworkCompat.sendToPlayer(CHANNEL, player, payload);
    }

    public static void sendToServer(Object payload) {
        NetworkCompat.sendToServer(CHANNEL, payload);
    }
}
