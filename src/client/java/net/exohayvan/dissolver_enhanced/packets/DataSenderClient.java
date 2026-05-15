package net.exohayvan.dissolver_enhanced.packets;

import java.util.List;

import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class DataSenderClient {
	public static void sendDataToServer(String messageId, String data) {
        ClientPayload payload = new ClientPayload(messageId, data);
		PacketDistributor.sendToServer(payload);
	}

	// HELPERS

    public static String listToString(List<String> items) {
        String stringList = "";

        for (String itemId : items) {
            stringList += itemId + ";;";
        }
        
        return stringList;
    }
}
