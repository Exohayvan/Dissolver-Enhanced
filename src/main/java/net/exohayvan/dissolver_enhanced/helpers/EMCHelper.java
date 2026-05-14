package net.exohayvan.dissolver_enhanced.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.exohayvan.dissolver_enhanced.packets.DataSender;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;
import net.minecraftforge.fml.ModList;

public class EMCHelper {
    public static boolean serverAddItem(Level world, String itemId, BigInteger addedValue) {
        MinecraftServer server = world.getServer();
        
        PlayerData globalData = StateSaverAndLoader.getGlobalData(server);
        List<String> learnedList = globalData.LEARNED_ITEMS;
        if (learnedList.contains(itemId)) return false;

        learnedList.add(itemId);
        StateSaverAndLoader.setGlobalLearned(server, learnedList);

        BigInteger newValue = EmcNumber.nonNegative(globalData.EMC).add(EmcNumber.nonNegative(addedValue));
        
        StateSaverAndLoader.setGlobalEMC(server, newValue);

        return true;
    }

    public static void addEMCValue(Player player, int addedValue) {
        addEMCValue(player, BigInteger.valueOf(addedValue));
    }

    public static void addEMCValue(Player player, BigInteger addedValue) {
        if (player.getServer() == null) return;

        BigInteger currentValue = getEMCValue(player);
        BigInteger newValue = currentValue.add(EmcNumber.nonNegative(addedValue));
        
        setEMCValue(player, newValue);
    }

    public static boolean removeEMCValue(Player player, int removedValue) {
        return removeEMCValue(player, BigInteger.valueOf(removedValue));
    }

    public static boolean removeEMCValue(Player player, BigInteger removedValue) {
        if (player.getServer() == null) return false;

        BigInteger currentValue = getEMCValue(player);
        BigInteger newValue = currentValue.subtract(EmcNumber.nonNegative(removedValue));

        if (newValue.signum() < 0) return false;

        setEMCValue(player, newValue);
        return true;
    }

    public static BigInteger getEMCValue(Player player) {
        return StateSaverAndLoader.getPlayerState(player).EMC;
    }

    public static void setEMCValue(Player player, BigInteger value) {
        StateSaverAndLoader.setPlayerEMC(player, value);
        ModCriteria.triggerEmcBalance(player, value);
    }

    // CHECK

    public enum Action {
        GET, ADD;
    }

    private static final long MISSING_ITEM_REPORT_COOLDOWN_MS = 30_000;
    private static final String MISSING_ITEM_TEMPLATE_URL = "https://github.com/ExoHayvan/Dissolver-Enhanced/issues/new?template=missing_item.yml";
    private static final int REPORT_FIELD_MAX_LENGTH = 1_500;
    private static final HashMap<String, Long> MISSING_ITEM_REPORT_TIMES = new HashMap<>();

    private static boolean checkValidEMC(int emc, String id, Action action) {
        return checkValidEMC(BigInteger.valueOf(emc), id, action);
    }

    private static boolean checkValidEMC(BigInteger emc, String id, Action action) {
        if (emc == null || emc.signum() == 0) {
            DissolverEnhanced.LOGGER.info("Tried to " + action + " item, but it does not have any EMC value. ID: " + id);
            return false;
        }

        return true;
    }

    private static BigInteger stackValue(BigInteger emcValue, int itemCount, double durabilityPercentage) {
        BigDecimal value = new BigDecimal(EmcNumber.nonNegative(emcValue))
            .multiply(BigDecimal.valueOf(itemCount))
            .multiply(BigDecimal.valueOf(durabilityPercentage));
        BigInteger rounded = value.toBigInteger();
        return rounded.signum() > 0 ? rounded : BigInteger.ONE;
    }

    // GET

    public static boolean getItem(Player player, ItemStack itemStack, DissolverScreenHandler handler, int items) {
        String itemId = EMCKey.fromStack(itemStack);
        BigInteger singleEmcValue = EMCValues.getBig(itemId);
        BigInteger emcValue = singleEmcValue.multiply(BigInteger.valueOf(items));

        if (!checkValidEMC(emcValue, itemId, Action.GET)) return false;

        if (!EMCHelper.removeEMCValue(player, emcValue)) {
            sendMessageToClient(player, "emc.action.not_enough_short");
            return false;
        }
        sendEmcDeltaToClient(player, emcValue.negate());
        captureDissolverItemExtracted(itemId, items, singleEmcValue, emcValue, isCreativeItem(itemId));

        // refresh block inv content
        new Thread(() -> {
            // let content update before updating!
            wait(10);
            handler.refresh();
        }).start();

        return true;
    }

    public static boolean addItem(ItemStack itemStack) {
        String itemId = EMCKey.fromStack(itemStack);
        BigInteger emcValue = EMCValues.getBig(itemId);

        return checkValidEMC(emcValue, itemId, Action.ADD);
    }

    public static boolean canAddItem(ItemStack itemStack, Player player) {
        String itemId = EMCKey.fromStack(itemStack);
        BigInteger emcValue = EMCValues.getBig(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) {
            captureDissolverItemRejected(itemId, rejectionReason(itemId));
            reportMissingItemValue(player, itemStack, itemId);
            return false;
        }

        return true;
    }

    // ADD

    // added from another inventory & not private EMC
    public static boolean addItem(ItemStack itemStack, Level world) {
        String itemId = EMCKey.fromStack(itemStack);
        BigInteger emcValue = EMCValues.getBig(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) {
            captureDissolverItemRejected(itemId, rejectionReason(itemId));
            return false;
        }

        int itemCount = itemStack.getCount();
        BigInteger addedEmcValue = stackValue(emcValue, itemCount, ItemHelper.getDurabilityPercentage(itemStack));

        boolean learned = serverAddItem(world, storageKey(itemId), addedEmcValue);
        if (learned) {
            captureDissolverItemDissolved(itemId, itemCount, emcValue, addedEmcValue, isCreativeItem(itemId));
            captureDissolverItemLearned(itemId, itemCount, emcValue, addedEmcValue, isCreativeItem(itemId));
        }
        return learned;
    }

    public static boolean addItem(ItemStack itemStack, Player player, DissolverScreenHandler handler) {
        String itemId = EMCKey.fromStack(itemStack);
        BigInteger emcValue = EMCValues.getBig(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) {
            captureDissolverItemRejected(itemId, rejectionReason(itemId));
            reportMissingItemValue(player, itemStack, itemId);
            return false;
        }

        // calculated new EMC (from DissolverInventoryInput)
        int itemCount = itemStack.getCount();
        BigInteger addedEmcValue = stackValue(emcValue, itemCount, ItemHelper.getDurabilityPercentage(itemStack));

        String learnedItemId = storageKey(itemId);
        boolean learned = !StateSaverAndLoader.getPlayerState(player).LEARNED_ITEMS.contains(learnedItemId);
        StateSaverAndLoader.addPlayerEMCAndLearned(player, learnedItemId, addedEmcValue);
        ModCriteria.triggerLearnedItem(player, itemId);
        ModCriteria.triggerLearnedCount(player, StateSaverAndLoader.getPlayerState(player).LEARNED_ITEMS.size());
        sendEmcDeltaToClient(player, addedEmcValue);

        captureDissolverItemDissolved(itemId, itemCount, emcValue, addedEmcValue, isCreativeItem(itemId));
        if (learned) {
            captureDissolverItemLearned(itemId, itemCount, emcValue, addedEmcValue, isCreativeItem(itemId));
        }

        // refresh block inv content
        new Thread(() -> {
            // let content update before updating!
            wait(10);
            handler.refresh();
        }).start();

        return true;
    }

    // LEARN

    public static boolean learnItem(Player player, String itemId) {
        return learnItem(player, itemId, true);
    }

    public static boolean learnItem(Player player, String itemId, boolean sendStoredMessage) {
        List<String> learnedList = StateSaverAndLoader.getPlayerState(player).LEARNED_ITEMS;
        if (learnedList.contains(itemId)) return false;

        learnedList.add(itemId);
        StateSaverAndLoader.setPlayerLearned(player, learnedList);
        ModCriteria.triggerLearnedCount(player, learnedList.size());

        if (sendStoredMessage) {
            // let blocklist update before sending message (to prevent empty message sent)
            new Thread(() -> {
                wait(50);
                sendMessageToClient(player, "emc.action.stored_short");
            }).start();
        }

        return true;
    }

    public static void reportMissingItemValue(Player player, ItemStack itemStack) {
        reportMissingItemValue(player, itemStack, EMCKey.fromStack(itemStack));
    }

    private static void reportMissingItemValue(Player player, ItemStack itemStack, String itemId) {
        String namespace = namespace(itemId);
        long now = System.currentTimeMillis();
        Long lastReportTime = MISSING_ITEM_REPORT_TIMES.get(namespace);

        if (lastReportTime != null && now - lastReportTime < MISSING_ITEM_REPORT_COOLDOWN_MS) {
            return;
        }

        MISSING_ITEM_REPORT_TIMES.put(namespace, now);

        String reportUrl = missingItemReportUrl(itemStack, itemId, namespace);
        String itemName = itemStack.getHoverName().getString();

        DissolverEnhanced.LOGGER.warn("Missing EMC value for item '{}' ({}) from namespace '{}'. Report link: {}", itemName, itemId, namespace, reportUrl);

        Component message = Component.literal("Dissolver could not determine an EMC value for " + itemName + " (" + itemId + "). ")
            .append(Component.literal("Open report")
                .withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, reportUrl))));

        player.sendSystemMessage(message);
    }

    private static String missingItemReportUrl(ItemStack itemStack, String itemId, String namespace) {
        return MISSING_ITEM_TEMPLATE_URL
            + "&title=" + urlEncode("[Missing Item]: " + itemId)
            + "&item_id=" + urlEncode(itemId)
            + "&item_name=" + urlEncode(itemStack.getHoverName().getString())
            + "&item_count=" + urlEncode(String.valueOf(itemStack.getCount()))
            + "&source_namespace=" + urlEncode(namespace)
            + "&minecraft_version=" + urlEncode(SharedConstants.getCurrentVersion().getName())
            + "&mod_version=" + urlEncode(modVersion(DissolverEnhanced.MOD_ID))
            + "&mod_loader=" + urlEncode("Forge")
            + "&loader_version=" + urlEncode(modVersion("forge"))
            + "&modpack=" + urlEncode(modpackDetails())
            + "&item_data=" + urlEncode(limitReportField(itemData(itemStack, itemId, namespace)))
            + "&item_tags=" + urlEncode(limitReportField(itemTags(itemStack)))
            + "&recipe_diagnostics=" + urlEncode(limitReportField(recipeDiagnostics(itemId)))
            + "&loaded_mods=" + urlEncode(limitReportField(loadedMods()))
            + "&config_snapshot=" + urlEncode(limitReportField(configSnapshot()))
            + "&log_excerpt=" + urlEncode(logExcerpt(itemStack, itemId, namespace))
            + "&generated_report=" + urlEncode(limitReportField(generatedMissingItemReport(itemStack, itemId, namespace)));
    }

    private static String generatedMissingItemReport(ItemStack itemStack, String itemId, String namespace) {
        return "{\n"
            + "  \"reason\": \"missing_emc_value\",\n"
            + "  \"itemId\": \"" + jsonEscape(itemId) + "\",\n"
            + "  \"itemName\": \"" + jsonEscape(itemStack.getHoverName().getString()) + "\",\n"
            + "  \"count\": " + itemStack.getCount() + ",\n"
            + "  \"sourceNamespace\": \"" + jsonEscape(namespace) + "\",\n"
            + "  \"minecraftVersion\": \"" + jsonEscape(SharedConstants.getCurrentVersion().getName()) + "\",\n"
            + "  \"modVersion\": \"" + jsonEscape(modVersion(DissolverEnhanced.MOD_ID)) + "\",\n"
            + "  \"loader\": \"Forge\",\n"
            + "  \"loaderVersion\": \"" + jsonEscape(modVersion("forge")) + "\",\n"
            + "  \"modpack\": \"" + jsonEscape(modpackDetails()) + "\",\n"
            + "  \"itemTags\": \"" + jsonEscape(itemTags(itemStack)) + "\",\n"
            + "  \"recipeDiagnostics\": \"" + jsonEscape(recipeDiagnostics(itemId)) + "\",\n"
            + "  \"configSnapshot\": \"" + jsonEscape(configSnapshot()) + "\"\n"
            + "}";
    }

    private static String itemData(ItemStack itemStack, String itemId, String namespace) {
        return "{\n"
            + "  \"id\": \"" + jsonEscape(itemId) + "\",\n"
            + "  \"baseItemId\": \"" + jsonEscape(EMCKey.baseItemId(itemId)) + "\",\n"
            + "  \"name\": \"" + jsonEscape(itemStack.getHoverName().getString()) + "\",\n"
            + "  \"count\": " + itemStack.getCount() + ",\n"
            + "  \"sourceNamespace\": \"" + jsonEscape(namespace) + "\",\n"
            + "  \"tag\": \"" + jsonEscape(String.valueOf(itemStack.getTag())) + "\"\n"
            + "}";
    }

    private static String itemTags(ItemStack itemStack) {
        String tags = itemStack
            .getTags()
            .map(TagKey::location)
            .map(id -> "#" + id)
            .sorted()
            .collect(Collectors.joining("\n"));

        return tags.isEmpty() ? "None" : tags;
    }

    private static String recipeDiagnostics(String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        List<String> lines = new ArrayList<>();

        lines.add("EMC key: " + itemId);
        lines.add("Base item ID: " + baseItemId);
        lines.add("EMC source: " + EMCValues.getSource(itemId));
        lines.add("EMC source detail: " + EMCValues.getSourceDetail(itemId));
        lines.add("Exact value exists: " + (EMCValues.hasExactValue(itemId) ? "yes" : "no"));
        lines.add("Base value exists: " + (EMCValues.hasExactValue(baseItemId) ? "yes" : "no"));
        lines.add("");
        lines.addAll(EMCValues.getRecipeDebugLines(baseItemId));

        return String.join("\n", lines);
    }

    private static String loadedMods() {
        return ModList.get()
            .getMods()
            .stream()
            .sorted(Comparator.comparing(mod -> mod.getModId()))
            .map(mod -> mod.getModId()
                + " "
                + mod.getVersion()
                + " - "
                + mod.getDisplayName())
            .collect(Collectors.joining("\n"));
    }

    private static String configSnapshot() {
        int overrideCount = ModConfig.EMC_OVERRIDES == null ? 0 : ModConfig.EMC_OVERRIDES.items().size() + ModConfig.EMC_OVERRIDES.tags().size();

        return "emc_on_hud=" + ModConfig.EMC_ON_HUD + "\n"
            + "private_emc=" + ModConfig.PRIVATE_EMC + "\n"
            + "creative_items=" + ModConfig.CREATIVE_ITEMS + "\n"
            + "difficulty=" + ModConfig.DIFFICULTY + "\n"
            + "mode=" + ModConfig.MODE + "\n"
            + "emc_overrides=" + overrideCount;
    }

    private static String logExcerpt(ItemStack itemStack, String itemId, String namespace) {
        return "Missing EMC value for item '" + itemStack.getHoverName().getString() + "' (" + itemId + ") from namespace '" + namespace + "'.\n"
            + "Cooldown key: " + namespace + "\n"
            + "Cooldown seconds: " + (MISSING_ITEM_REPORT_COOLDOWN_MS / 1000);
    }

    private static String modpackDetails() {
        return "Not detected automatically by Forge";
    }

    private static String namespace(String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? "unknown" : baseItemId.substring(0, namespaceEnd);
    }

    private static String itemName(String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? baseItemId : baseItemId.substring(namespaceEnd + 1);
    }

    private static void captureDissolverItemLearned(String itemId, int stackCount, BigInteger singleValue, BigInteger totalValue, boolean creativeItem) {
        String baseItemId = EMCKey.baseItemId(itemId);
        ModAnalytics.captureDissolverItemLearned(
            namespace(itemId),
            itemName(itemId),
            baseItemId,
            stackCount,
            singleValue,
            totalValue,
            creativeItem
        );
    }

    private static void captureDissolverItemDissolved(String itemId, int stackCount, BigInteger singleValue, BigInteger totalValue, boolean creativeItem) {
        String baseItemId = EMCKey.baseItemId(itemId);
        ModAnalytics.captureDissolverItemDissolved(
            namespace(itemId),
            itemName(itemId),
            baseItemId,
            stackCount,
            singleValue,
            totalValue,
            creativeItem
        );
    }

    private static void captureDissolverItemExtracted(String itemId, int stackCount, BigInteger singleValue, BigInteger totalValue, boolean creativeItem) {
        String baseItemId = EMCKey.baseItemId(itemId);
        ModAnalytics.captureDissolverItemExtracted(
            namespace(itemId),
            itemName(itemId),
            baseItemId,
            stackCount,
            singleValue,
            totalValue,
            creativeItem
        );
    }

    private static void captureDissolverItemRejected(String itemId, String reason) {
        String baseItemId = EMCKey.baseItemId(itemId);
        ModAnalytics.captureDissolverItemRejected(
            namespace(itemId),
            itemName(itemId),
            baseItemId,
            reason
        );
    }

    private static boolean isCreativeItem(String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        return baseItemId.contains("spawn_egg")
            || baseItemId.contains("command_block")
            || baseItemId.contains("bedrock")
            || baseItemId.contains("barrier")
            || baseItemId.contains("structure_block")
            || baseItemId.contains("jigsaw")
            || baseItemId.contains("spawner")
            || baseItemId.contains("vault")
            || baseItemId.contains("end_portal_frame")
            || baseItemId.contains("budding_amethyst")
            || baseItemId.contains("reinforced_deepslate");
    }

    private static String rejectionReason(String itemId) {
        if (isCreativeItem(itemId) && !ModConfig.CREATIVE_ITEMS) {
            return "creative_disabled";
        }

        return "no_emc";
    }

    private static String modVersion(String modId) {
        return ModList.get()
            .getModContainerById(modId)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String limitReportField(String value) {
        if (value.length() <= REPORT_FIELD_MAX_LENGTH) return value;

        return value.substring(0, REPORT_FIELD_MAX_LENGTH) + "\n... truncated by generated report link";
    }

    private static String jsonEscape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String storageKey(String itemId) {
        if (!EMCKey.isComponentKey(itemId) || EMCValues.hasExactValue(itemId)) {
            return itemId;
        }

        return EMCKey.baseItemId(itemId);
    }

    public static boolean forgetItem(Player player, String itemId) {
        List<String> learnedList = StateSaverAndLoader.getPlayerState(player).LEARNED_ITEMS;
        if (!learnedList.contains(itemId)) return false;

        learnedList.remove(itemId);
        StateSaverAndLoader.setPlayerLearned(player, learnedList);

        // let blocklist update before sending message (to prevent empty message sent)
        new Thread(() -> {
            wait(50);
            sendMessageToClient(player, "emc.action.removed_short");
        }).start();

        return true;
    }

    public static void learnAllItems(Player player) {
        List<String> learnedList = new ArrayList<>();

        for (String key : EMCValues.getList()) {
            learnedList.add(key);
        }
        
        StateSaverAndLoader.setPlayerLearned(player, learnedList);
    }

    public static void forgetAllItems(Player player) {
        StateSaverAndLoader.setPlayerLearned(player, new ArrayList<>());
    }

    // SEND

    public static void sendStateToClient(Player player) {
        PlayerData playerState = StateSaverAndLoader.getPlayerState(player);
        DataSender.sendPlayerData(player, playerState);
    }
    
    private static final HashMap<String, Integer> TIMEOUT_IDs = new HashMap<String, Integer>();
    public static void sendMessageToClient(Player player, String message) {
        PlayerData playerState = StateSaverAndLoader.getPlayerState(player);
        playerState.MESSAGE = message;
        DataSender.sendPlayerData(player, playerState);

        String playerId = player.getUUID().toString();
        if (!TIMEOUT_IDs.containsKey(playerId)) TIMEOUT_IDs.put(playerId, 0);
        int currentId = TIMEOUT_IDs.get(playerId) + 1;
        TIMEOUT_IDs.put(playerId, currentId);

        // timeout message
        new Thread(() -> {
            wait(1200);

            // another message was sent before this could clear!
            if (TIMEOUT_IDs.get(playerId) != currentId) return;

            PlayerData playerStateNew = StateSaverAndLoader.getPlayerState(player);
            playerStateNew.MESSAGE = "";
            DataSender.sendPlayerData(player, playerStateNew);
        }).start();
    }

    public static void sendEmcDeltaToClient(Player player, BigInteger delta) {
        String sign = delta.signum() >= 0 ? "+" : "-";
        String color = delta.signum() >= 0 ? "§a" : "§c";
        String value = EmcNumber.format(delta.abs());
        sendMessageToClient(player, "literal:" + color + sign + value + " EMC");
    }

    // TOOLTIP

    public static Component tooltipValue(String key) {
        return tooltipValue(key, 1);
    }

    public static Component tooltipValue(String key, double reducedEmc) {
        BigInteger EMC = EMCValues.getDisplayBig(key);
        Component text = Component.literal("");
        if (EMC.signum() == 0) return text;

        BigInteger value = new BigDecimal(EMC).multiply(BigDecimal.valueOf(reducedEmc)).toBigInteger();
        return Component.translatable("item_tooltip.dissolver_enhanced.emc", EmcNumber.format(value));
    }

    // HELPERS

    private static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
