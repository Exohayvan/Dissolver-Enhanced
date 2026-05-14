package net.exohayvan.dissolver_enhanced.analytics;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.common.analytics.LocalInstanceId;
import net.exohayvan.dissolver_enhanced.common.analytics.PostHogCaptureClient;
import net.exohayvan.dissolver_enhanced.common.analytics.PostHogErrorReporter;
import net.exohayvan.dissolver_enhanced.common.analytics.PostHogSessionId;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.config.SimpleConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ModAnalytics {
    private static final String STARTUP_EVENT = "mod_started";
    private static final String SESSION_STARTED_EVENT = "session_started";
    private static final String SESSION_ENDED_EVENT = "session_ended";
    private static final String HEARTBEAT_EVENT = "heartbeat";
    private static final String BLOCK_USED_EVENT = "custom_block_used";
    private static final String CONFIG_LOADED_EVENT = "config_loaded";
    private static final String ITEM_LEARNED_EVENT = "dissolver_item_learned";
    private static final String ITEM_DISSOLVED_EVENT = "dissolver_item_dissolved";
    private static final String ITEM_EXTRACTED_EVENT = "dissolver_item_extracted";
    private static final String ITEM_REJECTED_EVENT = "dissolver_item_rejected";
    private static final String ACHIEVEMENT_EARNED_EVENT = "achevement_earned";
    private static final int HEARTBEAT_INTERVAL_TICKS = 20 * 60;
    private static PostHogCaptureClient client;
    private static PostHogErrorReporter errorReporter;
    private static String distinctId;
    private static String sessionId;
    private static int serverHeartbeatTicks;
    private static Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;

    private ModAnalytics() {
    }

    public static void init() {
        if (!ModConfig.ANALYTICS_ENABLED) {
            DissolverEnhanced.LOGGER.info("Dissolver Enhanced analytics are disabled.");
            return;
        }

        if (ModConfig.ANALYTICS_PROJECT_TOKEN == null || ModConfig.ANALYTICS_PROJECT_TOKEN.isBlank()) {
            DissolverEnhanced.LOGGER.warn("Dissolver Enhanced analytics are enabled, but analytics_project_token is blank.");
            return;
        }

        try {
            distinctId = LocalInstanceId.readOrCreate(
                SimpleConfig.configDirectory().resolve("analytics-instance-id.txt")
            );
            sessionId = PostHogSessionId.create();
            client = new PostHogCaptureClient(
                ModConfig.ANALYTICS_ENDPOINT,
                ModConfig.ANALYTICS_PROJECT_TOKEN,
                (message, exception) -> DissolverEnhanced.LOGGER.warn(message, exception)
            );
            errorReporter = new PostHogErrorReporter(
                ModConfig.ANALYTICS_ERROR_ENDPOINT,
                ModConfig.ANALYTICS_PROJECT_TOKEN,
                distinctId,
                "net.exohayvan.dissolver_enhanced",
                ModAnalytics::baseEventProperties,
                (message, exception) -> DissolverEnhanced.LOGGER.warn(message, exception)
            );
            Runtime.getRuntime().addShutdownHook(new Thread(ModAnalytics::close, "dissolver-enhanced-analytics-shutdown"));
            installUncaughtExceptionHandler();
            client.capture(SESSION_STARTED_EVENT, distinctId, startupProperties());
            client.capture(STARTUP_EVENT, distinctId, startupProperties());
            captureConfigLoaded();
            registerServerHeartbeat();
            DissolverEnhanced.LOGGER.info("Queued Dissolver Enhanced analytics startup event.");
        } catch (IOException | IllegalArgumentException exception) {
            DissolverEnhanced.LOGGER.warn("Could not initialize Dissolver Enhanced analytics.", exception);
        }
    }

    public static boolean enabled() {
        return client != null && distinctId != null;
    }

    public static void captureClientHeartbeat(Map<String, Object> clientProperties) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = baseEventProperties();
        properties.putAll(clientProperties);
        client.capture(HEARTBEAT_EVENT, distinctId, properties);
    }

    public static void captureBlockUse(String blockId) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = baseEventProperties();
        properties.put("block_id", blockId);
        properties.put("event_side", "server");
        client.capture(BLOCK_USED_EVENT, distinctId, properties);
    }

    public static void captureConfigLoaded() {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = baseEventProperties();
        properties.put("event_side", environmentSide());
        properties.put("emc_on_hud", ModConfig.EMC_ON_HUD);
        properties.put("private_emc", ModConfig.PRIVATE_EMC);
        properties.put("creative_items", ModConfig.CREATIVE_ITEMS);
        properties.put("difficulty", ModConfig.DIFFICULTY);
        properties.put("mode", ModConfig.MODE);
        properties.put("emc_override_item_count", ModConfig.EMC_OVERRIDES == null ? 0 : ModConfig.EMC_OVERRIDES.items().size());
        properties.put("emc_override_tag_count", ModConfig.EMC_OVERRIDES == null ? 0 : ModConfig.EMC_OVERRIDES.tags().size());
        client.capture(CONFIG_LOADED_EVENT, distinctId, properties);
    }

    public static void captureDissolverItemLearned(
        String namespace,
        String item,
        String itemId,
        int stackCount,
        BigInteger singleValue,
        BigInteger totalValue,
        boolean creativeItem
    ) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = itemProperties(namespace, item, itemId);
        properties.put("stack_count", stackCount);
        properties.put("single_value", singleValue.toString());
        properties.put("total_value", totalValue.toString());
        properties.put("creative_item", creativeItem);
        client.capture(ITEM_LEARNED_EVENT, distinctId, properties);
    }

    public static void captureDissolverItemDissolved(
        String namespace,
        String item,
        String itemId,
        int stackCount,
        BigInteger singleValue,
        BigInteger totalValue,
        boolean creativeItem
    ) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = itemProperties(namespace, item, itemId);
        properties.put("stack_count", stackCount);
        properties.put("single_value", singleValue.toString());
        properties.put("total_value", totalValue.toString());
        properties.put("creative_item", creativeItem);
        client.capture(ITEM_DISSOLVED_EVENT, distinctId, properties);
    }

    public static void captureDissolverItemExtracted(
        String namespace,
        String item,
        String itemId,
        int stackCount,
        BigInteger singleValue,
        BigInteger totalValue,
        boolean creativeItem
    ) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = itemProperties(namespace, item, itemId);
        properties.put("stack_count", stackCount);
        properties.put("single_value", singleValue.toString());
        properties.put("total_value", totalValue.toString());
        properties.put("creative_item", creativeItem);
        client.capture(ITEM_EXTRACTED_EVENT, distinctId, properties);
    }

    public static void captureDissolverItemRejected(String namespace, String item, String itemId, String reason) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = itemProperties(namespace, item, itemId);
        properties.put("reason", reason);
        client.capture(ITEM_REJECTED_EVENT, distinctId, properties);
    }

    public static void captureAchievementEarned(String achievementId, Map<String, Object> achievementProperties) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = baseEventProperties();
        properties.put("event_side", "server");
        properties.put("achievement_id", achievementId);
        properties.putAll(achievementProperties);
        client.capture(ACHIEVEMENT_EARNED_EVENT, distinctId, properties);
    }

    public static void captureException(Throwable throwable, boolean handled) {
        if (errorReporter != null && throwable != null) {
            errorReporter.capture(throwable, handled);
        }
    }

    private static void installUncaughtExceptionHandler() {
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (errorReporter != null && throwable != null) {
                try {
                    errorReporter.capture(throwable, false).get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    DissolverEnhanced.LOGGER.warn("Could not flush uncaught exception analytics.", e);
                }
            }
            if (previousUncaughtExceptionHandler != null) {
                previousUncaughtExceptionHandler.uncaughtException(thread, throwable);
            }
        });
    }

    private static void registerServerHeartbeat() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverHeartbeatTicks++;
            if (serverHeartbeatTicks < HEARTBEAT_INTERVAL_TICKS) {
                return;
            }

            serverHeartbeatTicks = 0;
            captureServerHeartbeat(server);
        });
    }

    private static void captureServerHeartbeat(MinecraftServer server) {
        if (!enabled()) {
            return;
        }

        Map<String, Object> properties = baseEventProperties();
        properties.put("event_side", "server");
        properties.put("world_state", "server");
        properties.put("world_info", "server");
        properties.put("session_location", "server");
        properties.put("player_count", server.getPlayerCount());
        properties.put("emc_storage_mode", ModConfig.PRIVATE_EMC ? "private_total" : "shared");

        ServerTotals totals = serverTotals(server);
        properties.put("emc_value", totals.emc().toString());
        properties.put("stored_item_count", totals.storedItemCount());

        client.capture(HEARTBEAT_EVENT, distinctId, properties);
    }

    private static ServerTotals serverTotals(MinecraftServer server) {
        StateSaverAndLoader state = StateSaverAndLoader.getServerState(server);
        if (!ModConfig.PRIVATE_EMC) {
            return new ServerTotals(state.sharedData.EMC, state.sharedData.LEARNED_ITEMS.size());
        }

        BigInteger emc = BigInteger.ZERO;
        int storedItemCount = 0;
        for (PlayerData playerData : state.players.values()) {
            emc = emc.add(playerData.EMC);
            storedItemCount += playerData.LEARNED_ITEMS.size();
        }

        return new ServerTotals(emc, storedItemCount);
    }

    private static Map<String, Object> startupProperties() {
        Map<String, Object> properties = baseEventProperties();
        properties.put("event_side", environmentSide());
        return properties;
    }

    private static Map<String, Object> baseEventProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        String modVersion = modVersion(DissolverEnhanced.MOD_ID);
        String minecraftVersion = modVersion("minecraft");
        String loader = "fabric";
        properties.put("mod_id", DissolverEnhanced.MOD_ID);
        properties.put("mod_version", modVersion);
        properties.put("minecraft_version", minecraftVersion);
        properties.put("loader", loader);
        properties.put("loader_minecraft", loader + "-" + minecraftVersion);
        properties.put("loader_version", modVersion("fabricloader"));
        properties.put("runtime_side", environmentSide());
        properties.put("analytics_enabled", ModConfig.ANALYTICS_ENABLED);
        if (sessionId != null) {
            properties.put("$session_id", sessionId);
            properties.put("session_id", sessionId);
        }
        properties.put("java_version", System.getProperty("java.version"));
        properties.put("os_name", System.getProperty("os.name"));
        return properties;
    }

    private static Map<String, Object> itemProperties(String namespace, String item, String itemId) {
        Map<String, Object> properties = baseEventProperties();
        properties.put("event_side", "server");
        properties.put("namespace", namespace);
        properties.put("item", item);
        properties.put("item_id", itemId);
        return properties;
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    private static String environmentSide() {
        EnvType environmentType = FabricLoader.getInstance().getEnvironmentType();
        return environmentType == EnvType.CLIENT ? "client" : "dedicated_server";
    }

    private static void close() {
        if (client != null) {
            try {
                Map<String, Object> properties = startupProperties();
                properties.put("session_location", environmentSide().equals("client") ? "menu" : "server");
                client.capture(SESSION_ENDED_EVENT, distinctId, properties).get(2, TimeUnit.SECONDS);
            } catch (Exception exception) {
                DissolverEnhanced.LOGGER.warn("Could not flush session ended analytics.", exception);
            }
            client.close();
        }
        if (errorReporter != null) {
            errorReporter.close();
        }
    }

    private record ServerTotals(BigInteger emc, int storedItemCount) {
    }
}
