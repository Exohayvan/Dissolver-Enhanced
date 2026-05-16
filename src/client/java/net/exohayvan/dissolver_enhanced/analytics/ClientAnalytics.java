package net.exohayvan.dissolver_enhanced.analytics;

import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientAnalytics {
    private static final int HEARTBEAT_INTERVAL_TICKS = 20 * 60;
    private static int heartbeatTicks;

    private ClientAnalytics() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ClientAnalytics::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        heartbeatTicks++;
        if (heartbeatTicks < HEARTBEAT_INTERVAL_TICKS) {
            return;
        }

        heartbeatTicks = 0;
        captureHeartbeat(Minecraft.getInstance());
    }

    private static void captureHeartbeat(Minecraft client) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("event_side", "client");

        if (client.level == null || client.player == null) {
            properties.put("world_state", "menus");
            properties.put("world_info", "menus");
            properties.put("world_mode", "menu");
            properties.put("game_mode", "menu");
            properties.put("session_location", "menu");
            properties.put("emc_value", "0");
            properties.put("stored_item_count", 0);
            ModAnalytics.captureClientHeartbeat(properties);
            return;
        }

        String worldMode = client.hasSingleplayerServer() ? "singleplayer" : "multiplayer";
        properties.put("world_state", "in_world");
        properties.put("world_info", client.level.dimension().toString());
        properties.put("world_mode", worldMode);
        properties.put("game_mode", playerGameMode(client));
        properties.put("session_location", worldMode + "_world");
        properties.put("emc_value", PlayerDataClient.EMC.toString());
        properties.put("stored_item_count", PlayerDataClient.LEARNED_ITEMS_SIZE);
        ModAnalytics.captureClientHeartbeat(properties);
    }

    private static String playerGameMode(Minecraft client) {
        if (client.player.isSpectator()) {
            return "spectator";
        }
        return client.player.getAbilities().instabuild ? "creative" : "survival";
    }
}
