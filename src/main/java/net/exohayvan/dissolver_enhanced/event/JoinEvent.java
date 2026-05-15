package net.exohayvan.dissolver_enhanced.event;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValueUpdateMonitor;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.exohayvan.dissolver_enhanced.packets.DataSender;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DissolverEnhanced.MOD_ID)
public class JoinEvent {
    public static void init() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerData playerState = StateSaverAndLoader.getPlayerState(player);
        DataSender.sendPlayerData(player, playerState);
        sendPlayerDataWhenEMCValuesReady(player);
        sendDefaultEmcUpdateMessage(player);
    }

    private static void sendDefaultEmcUpdateMessage(ServerPlayer player) {
        if (!DefaultEmcValueUpdateMonitor.hasUpdateAvailable()) {
            return;
        }

        DissolverEnhanced.LOGGER.info(DefaultEmcValueUpdateMonitor.UPDATE_MESSAGE);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(DefaultEmcValueUpdateMonitor.UPDATE_MESSAGE));
    }

    private static void sendPlayerDataWhenEMCValuesReady(ServerPlayer playerEntity) {
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
