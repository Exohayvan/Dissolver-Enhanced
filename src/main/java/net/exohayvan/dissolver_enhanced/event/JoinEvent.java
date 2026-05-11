package net.exohayvan.dissolver_enhanced.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.exohayvan.dissolver_enhanced.packets.DataSender;
import net.exohayvan.dissolver_enhanced.packets.clientbound.PlayerDataPayload;

public class JoinEvent {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataPayload.class);

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity playerEntity = handler.getPlayer();

            if (ServerPlayNetworking.canSend(playerEntity, PlayerDataPayload.ID)) {
                PlayerData playerState = StateSaverAndLoader.getPlayerState(playerEntity);
                DataSender.sendPlayerData(playerEntity, playerState);
                sendPlayerDataWhenEMCValuesReady(playerEntity);
            } else {
                LOGGER.error("Client cannot receive packet. This probably means that DissolverEnhanced is not installed on the client.");
                handler.disconnect(Text.literal("Please install the DissolverEnhanced mod to play on this server."));
            }
        });
    }

    private static void sendPlayerDataWhenEMCValuesReady(ServerPlayerEntity playerEntity) {
        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                if (EMCValues.getSyncVersion() > 0) {
                    PlayerData playerState = StateSaverAndLoader.getPlayerState(playerEntity);
                    DataSender.sendPlayerData(playerEntity, playerState);
                    return;
                }

                wait(100);
            }
        }).start();
    }

    private static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
