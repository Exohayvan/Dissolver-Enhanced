package net.exohayvan.dissolver_enhanced.packets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.packets.clientbound.EMCValuesPayload;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;

public class DataReceiverClient {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(EMCValuesPayload.ID, (payload, context) -> {
            if (Minecraft.getInstance().getSingleplayerServer() != null) return;

            EMCValues.applyClientSyncValues(payload.values());
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerDataPayload.ID, (payload, context) -> {
			Player player = context.player();
            PlayerDataPayload playerData = payload;

            if (player == null) {
                DissolverEnhanced.LOGGER.error("Something went wrong! No client player receiver.");
                return;
            }

			receivedData(player, playerData);
        });
    }

	private static void receivedData(Player player, PlayerDataPayload playerData) {
			PlayerDataClient.EMC = net.exohayvan.dissolver_enhanced.common.values.EmcNumber.parse(playerData.emc());
			PlayerDataClient.LEARNED_ITEMS_SIZE = playerData.learnedItemsSize();
			PlayerDataClient.LEARNED_ITEMS_TOTAL_SIZE = playerData.learnedItemsTotalSize();
			PlayerDataClient.MESSAGE = playerData.message();
			PlayerDataClient.LEARNED_ITEMS = playerData.learnedItems();
		}

	// HELPERS

	public static List<String> stringToList(String stringList) {
		if (stringList.length() == 0) return new ArrayList<>();
		List<String> list = Arrays.asList(stringList.split(";;"));
		// list.remove(list.size() - 1); // last one is always empty (but automatically removed!)

		return list;
	}
}
