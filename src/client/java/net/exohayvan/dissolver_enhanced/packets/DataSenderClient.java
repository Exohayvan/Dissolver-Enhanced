package net.exohayvan.dissolver_enhanced.packets;

import java.util.List;

import net.exohayvan.dissolver_enhanced.packets.Packets;
import net.exohayvan.dissolver_enhanced.packets.serverbound.ClientPayload;

public class DataSenderClient {
	public static void sendDataToServer(String messageId, String data) {
        ClientPayload payload = new ClientPayload(messageId, data);
		Packets.CHANNEL.sendToServer(payload);
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
