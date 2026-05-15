package net.exohayvan.dissolver_enhanced.packets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.helpers.ServerCompat;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;

public class DataSender {
    private static final Map<UUID, Integer> EMC_SYNC_VERSIONS = new HashMap<>();

    public static void sendPlayerData(PlayerEntity player, PlayerData data) {
        MinecraftServer server = ServerCompat.getServer(player);
        ServerPlayerEntity playerEntity = getServerPlayer(server, player);
        if (playerEntity == null) return;

        int learnedItemsTotalSize = data.LEARNED_ITEMS_TOTAL_SIZE > 0 ? data.LEARNED_ITEMS_TOTAL_SIZE : data.LEARNED_ITEMS.size();
        PlayerDataPayload dataToSend = new PlayerDataPayload(data.EMC.toString(), data.LEARNED_ITEMS.size(), learnedItemsTotalSize, data.MESSAGE, data.LEARNED_ITEMS);
        EMCValuesPayload emcValuesToSend = getEMCValuesPayload(playerEntity);

        server.execute(() -> {
            if (emcValuesToSend != null) {
                ServerPlayNetworking.send(playerEntity, emcValuesToSend);
            }
            ServerPlayNetworking.send(playerEntity, dataToSend);
        });
    }

    // HELPERS

    private static ServerPlayerEntity getServerPlayer(MinecraftServer server, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) return (ServerPlayerEntity)player;
        if (server == null || !(player instanceof PlayerEntity)) return null;

        ServerPlayerEntity playerEntity = server.getPlayerManager().getPlayer(player.getUuid());
        if (!(playerEntity instanceof ServerPlayerEntity)) return null;

        return playerEntity;
    }

    private static EMCValuesPayload getEMCValuesPayload(ServerPlayerEntity player) {
        int syncVersion = EMCValues.getSyncVersion();
        if (syncVersion <= 0) return null;

        Integer playerSyncVersion = EMC_SYNC_VERSIONS.get(player.getUuid());
        if (playerSyncVersion != null && playerSyncVersion == syncVersion) return null;

        EMC_SYNC_VERSIONS.put(player.getUuid(), syncVersion);
        return new EMCValuesPayload(syncVersion, EMCValues.getSyncValues());
    }

    // private static String listToString(List<String> items) {
    //     String stringList = "";

    //     for (String itemId : items) {
    //         stringList += itemId + ";;";
    //     }
        
    //     return stringList;
    // }
}
