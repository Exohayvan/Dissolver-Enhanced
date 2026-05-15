package net.exohayvan.dissolver_enhanced.packets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class DataSender {
    private static final Map<UUID, Integer> EMC_SYNC_VERSIONS = new HashMap<>();

    public static void sendPlayerData(Player player, PlayerData data) {
        MinecraftServer server = player.getServer();
        ServerPlayer playerEntity = getServerPlayer(server, player);
        if (playerEntity == null) return;

        int learnedItemsTotalSize = data.LEARNED_ITEMS_TOTAL_SIZE > 0 ? data.LEARNED_ITEMS_TOTAL_SIZE : data.LEARNED_ITEMS.size();
        PlayerDataPayload dataToSend = new PlayerDataPayload(data.EMC.toString(), data.LEARNED_ITEMS.size(), learnedItemsTotalSize, data.MESSAGE, data.LEARNED_ITEMS);
        EMCValuesPayload emcValuesToSend = getEMCValuesPayload(playerEntity);

        server.execute(() -> {
            if (emcValuesToSend != null) {
                PacketDistributor.sendToPlayer(playerEntity, emcValuesToSend);
            }
            PacketDistributor.sendToPlayer(playerEntity, dataToSend);
        });
    }

    // HELPERS

    private static ServerPlayer getServerPlayer(MinecraftServer server, Player player) {
        if (player instanceof ServerPlayer) return (ServerPlayer)player;
        if (server == null || !(player instanceof Player)) return null;

        ServerPlayer playerEntity = server.getPlayerList().getPlayer(player.getUUID());
        if (!(playerEntity instanceof ServerPlayer)) return null;

        return playerEntity;
    }

    private static EMCValuesPayload getEMCValuesPayload(ServerPlayer player) {
        int syncVersion = EMCValues.getSyncVersion();
        if (syncVersion <= 0) return null;

        Integer playerSyncVersion = EMC_SYNC_VERSIONS.get(player.getUUID());
        if (playerSyncVersion != null && playerSyncVersion == syncVersion) return null;

        EMC_SYNC_VERSIONS.put(player.getUUID(), syncVersion);
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
