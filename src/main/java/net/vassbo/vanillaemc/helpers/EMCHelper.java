package net.vassbo.vanillaemc.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.SharedConstants;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.vassbo.vanillaemc.VanillaEMC;
import net.vassbo.vanillaemc.config.ModConfig;
import net.vassbo.vanillaemc.data.EMCValues;
import net.vassbo.vanillaemc.data.PlayerData;
import net.vassbo.vanillaemc.data.StateSaverAndLoader;
import net.vassbo.vanillaemc.packets.DataSender;
import net.vassbo.vanillaemc.screen.DissolverScreenHandler;

public class EMCHelper {
    public static boolean serverAddItem(World world, String itemId, int addedValue) {
        MinecraftServer server = world.getServer();
        
        PlayerData globalData = StateSaverAndLoader.getGlobalData(server);
        List<String> learnedList = globalData.LEARNED_ITEMS;
        if (learnedList.contains(itemId)) return false;

        learnedList.add(itemId);
        StateSaverAndLoader.setGlobalLearned(server, learnedList);

        int currentValue = globalData.EMC;
        int newValue = currentValue += addedValue;
        
        StateSaverAndLoader.setGlobalEMC(server, newValue);

        return true;
    }

    public static void addEMCValue(PlayerEntity player, int addedValue) {
        if (player.getServer() == null) return;

        int currentValue = getEMCValue(player);
        int newValue = currentValue += addedValue;
        
        setEMCValue(player, newValue);
    }

    public static boolean removeEMCValue(PlayerEntity player, int removedValue) {
        if (player.getServer() == null) return false;

        int currentValue = getEMCValue(player);
        int newValue = currentValue -= removedValue;

        if (newValue < 0) return false;

        setEMCValue(player, newValue);
        return true;
    }

    public static int getEMCValue(PlayerEntity player) {
        return StateSaverAndLoader.getPlayerState(player).EMC;
    }

    public static void setEMCValue(PlayerEntity player, int value) {
        StateSaverAndLoader.setPlayerEMC(player, value);
    }

    // CHECK

    public enum Action {
        GET, ADD;
    }

    private static final long MISSING_ITEM_REPORT_COOLDOWN_MS = 30_000;
    private static final String MISSING_ITEM_TEMPLATE_URL = "https://github.com/Exohayvan/Dissolver-Enhanced/issues/new?template=missing_item.yml";
    private static final int REPORT_FIELD_MAX_LENGTH = 1_500;
    private static final HashMap<String, Long> MISSING_ITEM_REPORT_TIMES = new HashMap<>();

    private static boolean checkValidEMC(int emc, String id, Action action) {
        if (emc == 0) {
            VanillaEMC.LOGGER.info("Tried to " + action + " item, but it does not have any EMC value. ID: " + id);
            return false;
        }

        return true;
    }

    // GET

    public static boolean getItem(PlayerEntity player, ItemStack itemStack, DissolverScreenHandler handler, int items) {
        String itemId = EMCKey.fromStack(itemStack);
        int emcValue = EMCValues.get(itemId) * items;

        if (!checkValidEMC(emcValue, itemId, Action.GET)) return false;

        if (!EMCHelper.removeEMCValue(player, emcValue)) {
            sendMessageToClient(player, "emc.action.not_enough_short");
            return false;
        }

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
        int emcValue = EMCValues.get(itemId);

        return checkValidEMC(emcValue, itemId, Action.ADD);
    }

    public static boolean canAddItem(ItemStack itemStack, PlayerEntity player) {
        String itemId = EMCKey.fromStack(itemStack);
        int emcValue = EMCValues.get(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) {
            reportMissingItemValue(player, itemStack, itemId);
            return false;
        }

        return true;
    }

    // ADD

    // added from another inventory & not private EMC
    public static boolean addItem(ItemStack itemStack, World world) {
        String itemId = EMCKey.fromStack(itemStack);
        int emcValue = EMCValues.get(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) return false;

        int itemCount = itemStack.getCount();
        int addedEmcValue = (int)(emcValue * itemCount * ItemHelper.getDurabilityPercentage(itemStack));

        return serverAddItem(world, storageKey(itemId), addedEmcValue);
    }

    public static boolean addItem(ItemStack itemStack, PlayerEntity player, DissolverScreenHandler handler) {
        String itemId = EMCKey.fromStack(itemStack);
        int emcValue = EMCValues.get(itemId);

        if (!checkValidEMC(emcValue, itemId, Action.ADD)) {
            reportMissingItemValue(player, itemStack, itemId);
            return false;
        }

        // calculated new EMC (from DissolverInventoryInput)
        int itemCount = itemStack.getCount();
        int addedEmcValue = (int)(emcValue * itemCount * ItemHelper.getDurabilityPercentage(itemStack));

        learnItem(player, storageKey(itemId));

        EMCHelper.addEMCValue(player, addedEmcValue);

        // refresh block inv content
        new Thread(() -> {
            // let content update before updating!
            wait(10);
            handler.refresh();
        }).start();

        return true;
    }

    // LEARN

    public static boolean learnItem(PlayerEntity player, String itemId) {
        List<String> learnedList = StateSaverAndLoader.getPlayerState(player).LEARNED_ITEMS;
        if (learnedList.contains(itemId)) return false;

        learnedList.add(itemId);
        StateSaverAndLoader.setPlayerLearned(player, learnedList);

        // let blocklist update before sending message (to prevent empty message sent)
        new Thread(() -> {
            wait(50);
            sendMessageToClient(player, "emc.action.stored_short");
        }).start();

        return true;
    }

    public static void reportMissingItemValue(PlayerEntity player, ItemStack itemStack) {
        reportMissingItemValue(player, itemStack, EMCKey.fromStack(itemStack));
    }

    private static void reportMissingItemValue(PlayerEntity player, ItemStack itemStack, String itemId) {
        String namespace = namespace(itemId);
        long now = System.currentTimeMillis();
        Long lastReportTime = MISSING_ITEM_REPORT_TIMES.get(namespace);

        if (lastReportTime != null && now - lastReportTime < MISSING_ITEM_REPORT_COOLDOWN_MS) {
            return;
        }

        MISSING_ITEM_REPORT_TIMES.put(namespace, now);

        String reportUrl = missingItemReportUrl(itemStack, itemId, namespace);
        String itemName = itemStack.getName().getString();

        VanillaEMC.LOGGER.warn("Missing EMC value for item '{}' ({}) from namespace '{}'. Report link: {}", itemName, itemId, namespace, reportUrl);

        Text message = Text.literal("Dissolver could not determine an EMC value for " + itemName + " (" + itemId + "). ")
            .append(Text.literal("Open report")
                .styled(style -> style
                    .withColor(Formatting.AQUA)
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, reportUrl))));

        player.sendMessage(message, false);
    }

    private static String missingItemReportUrl(ItemStack itemStack, String itemId, String namespace) {
        return MISSING_ITEM_TEMPLATE_URL
            + "&title=" + urlEncode("[Missing Item]: " + itemId)
            + "&item_id=" + urlEncode(itemId)
            + "&item_name=" + urlEncode(itemStack.getName().getString())
            + "&item_count=" + urlEncode(String.valueOf(itemStack.getCount()))
            + "&source_namespace=" + urlEncode(namespace)
            + "&minecraft_version=" + urlEncode(SharedConstants.getGameVersion().getName())
            + "&mod_version=" + urlEncode(modVersion(VanillaEMC.MOD_ID))
            + "&mod_loader=" + urlEncode("Fabric")
            + "&loader_version=" + urlEncode(modVersion("fabricloader"))
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
            + "  \"itemName\": \"" + jsonEscape(itemStack.getName().getString()) + "\",\n"
            + "  \"count\": " + itemStack.getCount() + ",\n"
            + "  \"sourceNamespace\": \"" + jsonEscape(namespace) + "\",\n"
            + "  \"minecraftVersion\": \"" + jsonEscape(SharedConstants.getGameVersion().getName()) + "\",\n"
            + "  \"modVersion\": \"" + jsonEscape(modVersion(VanillaEMC.MOD_ID)) + "\",\n"
            + "  \"loader\": \"Fabric\",\n"
            + "  \"loaderVersion\": \"" + jsonEscape(modVersion("fabricloader")) + "\",\n"
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
            + "  \"name\": \"" + jsonEscape(itemStack.getName().getString()) + "\",\n"
            + "  \"count\": " + itemStack.getCount() + ",\n"
            + "  \"sourceNamespace\": \"" + jsonEscape(namespace) + "\",\n"
            + "  \"components\": \"" + jsonEscape(itemStack.getComponentChanges().toString()) + "\"\n"
            + "}";
    }

    private static String itemTags(ItemStack itemStack) {
        String tags = itemStack
            .streamTags()
            .map(TagKey::id)
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
        return FabricLoader.getInstance()
            .getAllMods()
            .stream()
            .sorted(Comparator.comparing(container -> container.getMetadata().getId()))
            .map(container -> container.getMetadata().getId()
                + " "
                + container.getMetadata().getVersion().getFriendlyString()
                + " - "
                + container.getMetadata().getName())
            .collect(Collectors.joining("\n"));
    }

    private static String configSnapshot() {
        int overrideCount = ModConfig.EMC_OVERRIDES == null ? 0 : ModConfig.EMC_OVERRIDES.size();

        return "emc_on_hud=" + ModConfig.EMC_ON_HUD + "\n"
            + "private_emc=" + ModConfig.PRIVATE_EMC + "\n"
            + "creative_items=" + ModConfig.CREATIVE_ITEMS + "\n"
            + "difficulty=" + ModConfig.DIFFICULTY + "\n"
            + "mode=" + ModConfig.MODE + "\n"
            + "emc_overrides=" + overrideCount;
    }

    private static String logExcerpt(ItemStack itemStack, String itemId, String namespace) {
        return "Missing EMC value for item '" + itemStack.getName().getString() + "' (" + itemId + ") from namespace '" + namespace + "'.\n"
            + "Cooldown key: " + namespace + "\n"
            + "Cooldown seconds: " + (MISSING_ITEM_REPORT_COOLDOWN_MS / 1000);
    }

    private static String modpackDetails() {
        return "Not detected automatically by Fabric Loader";
    }

    private static String namespace(String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? "unknown" : baseItemId.substring(0, namespaceEnd);
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
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

    public static boolean forgetItem(PlayerEntity player, String itemId) {
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

    public static void learnAllItems(PlayerEntity player) {
        List<String> learnedList = new ArrayList<>();

        for (String key : EMCValues.getList()) {
            learnedList.add(key);
        }
        
        StateSaverAndLoader.setPlayerLearned(player, learnedList);
    }

    public static void forgetAllItems(PlayerEntity player) {
        StateSaverAndLoader.setPlayerLearned(player, new ArrayList<>());
    }

    // SEND

    public static void sendStateToClient(PlayerEntity player) {
        PlayerData playerState = StateSaverAndLoader.getPlayerState(player);
        DataSender.sendPlayerData(player, playerState);
    }
    
    private static final HashMap<String, Integer> TIMEOUT_IDs = new HashMap<String, Integer>();
    public static void sendMessageToClient(PlayerEntity player, String message) {
        PlayerData playerState = StateSaverAndLoader.getPlayerState(player);
        playerState.MESSAGE = message;
        DataSender.sendPlayerData(player, playerState);

        String playerId = player.getUuid().toString();
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

    // TOOLTIP

    public static Text tooltipValue(String key) {
        return tooltipValue(key, 1);
    }

    public static Text tooltipValue(String key, double reducedEmc) {
        Integer EMC = EMCValues.getDisplay(key);
        Text text = Text.literal("");
        if (EMC == 0) return text;

        return Text.translatable("item_tooltip.vanillaemc.emc", (int)(EMC * reducedEmc));
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
