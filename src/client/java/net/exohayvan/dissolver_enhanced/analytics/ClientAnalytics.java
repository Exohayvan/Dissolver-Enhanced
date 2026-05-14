package net.exohayvan.dissolver_enhanced.analytics;

import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientAnalytics {
    private static final int HEARTBEAT_INTERVAL_TICKS = 20 * 60;
    private static int heartbeatTicks;

    private ClientAnalytics() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            heartbeatTicks++;
            if (heartbeatTicks < HEARTBEAT_INTERVAL_TICKS) {
                return;
            }

            heartbeatTicks = 0;
            captureHeartbeat(client);
        });
    }

    private static void captureHeartbeat(MinecraftClient client) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("event_side", "client");

        if (client.world == null || client.player == null) {
            properties.put("world_state", "menus");
            properties.put("world_info", "menus");
            properties.put("world_mode", "menu");
            properties.put("session_location", "menu");
            properties.put("emc_value", "0");
            properties.put("stored_item_count", 0);
            ModAnalytics.captureClientHeartbeat(properties);
            return;
        }

        String worldMode = client.isIntegratedServerRunning() ? "singleplayer" : "multiplayer";
        properties.put("world_state", "in_world");
        properties.put("world_info", client.world.getRegistryKey().getValue().toString());
        properties.put("world_mode", worldMode);
        properties.put("session_location", worldMode + "_world");
        properties.put("emc_value", PlayerDataClient.EMC.toString());
        properties.put("stored_item_count", PlayerDataClient.LEARNED_ITEMS_SIZE);
        ModAnalytics.captureClientHeartbeat(properties);
    }
}
