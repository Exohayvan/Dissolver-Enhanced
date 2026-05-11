package net.exohayvan.dissolver_enhanced.packets;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;

public class DataReceiver {
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ClientPayload.ID, (payload, context) -> {
			MinecraftServer server = context.server();
			PlayerEntity player = context.player();
            String messageId = payload.messageId();
            String data = payload.data();

            if (server == null) {
                DissolverEnhanced.LOGGER.error("Something went wrong! Could not get server.");
                return;
            }

			receivedData(player, messageId, data);
        });
    }

	private static void receivedData(PlayerEntity player, String messageId, String data) {
        if (messageId.contains("search")) {
            ModScreenHandlers.activeHandlers.get(player.getUuid()).search(data);
        } else if (messageId.contains("scroll")) {
            ModScreenHandlers.activeHandlers.get(player.getUuid()).scrollItems(stringToFloat(data));
        } else {
            DissolverEnhanced.LOGGER.info("RECEIVED MESSAGE FROM CLIENT: " + messageId);
        }
	}

	// HELPERS

	// private static List<String> stringToList(String stringList) {
	// 	if (stringList.length() == 0) return new ArrayList<>();
	// 	List<String> list = Arrays.asList(stringList.split(";;"));
	// 	// list.remove(list.size() - 1); // last one is always empty (but automatically removed!)

	// 	return list;
	// }

    private static Float stringToFloat(String string) {
        return Float.parseFloat(string);
    }
}
