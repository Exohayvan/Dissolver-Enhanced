package net.exohayvan.dissolver_enhanced.config;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValues;
import net.exohayvan.dissolver_enhanced.common.values.EmcValueSet;
import net.exohayvan.dissolver_enhanced.common.config.ConfigConstants;
import net.exohayvan.dissolver_enhanced.common.config.ConfigEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ModConfig {
    private static final String CONFIG_FILE_NAME = "dissolver_enhanced";
    private static final String DEFAULT_EMC_FILE_NAME = "default-emc-values.yaml";
    private static final String EMC_OVERRIDES_FILE_NAME = "emc-overrides.yaml";

    public static SimpleConfig CONFIG;
    protected static ModConfigProvider configs;

    public static boolean EMC_ON_HUD;
    public static boolean PRIVATE_EMC;
    public static boolean CREATIVE_ITEMS;
    public static String DIFFICULTY;
    public static String MODE;
    public static boolean ANALYTICS_ENABLED;
    public static String ANALYTICS_ENDPOINT;
    public static String ANALYTICS_ERROR_ENDPOINT;
    public static String ANALYTICS_PROJECT_TOKEN;
    public static EmcValueSet DEFAULT_EMC_VALUES;
    public static EmcValueSet EMC_OVERRIDES;
    private static Path defaultValuesFile;
    private static Path overridesFile;

    public static void init() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(CONFIG_FILE_NAME).provider(configs).request();
        ensureAnalyticsConfigEntries();

        assignConfigs();
    }

    private static void createConfigs() {
        for (ConfigConstants value : ConfigConstants.values()) {
            configs.addKeyValuePair(value.asConfigEntry(), value.getComment());
        }
    }

    private static void assignConfigs() {
        EMC_ON_HUD = CONFIG.getOrDefault("emc_on_hud", false);
        PRIVATE_EMC = CONFIG.getOrDefault("private_emc", false);
        CREATIVE_ITEMS = CONFIG.getOrDefault("creative_items", false);
        DIFFICULTY = CONFIG.getOrDefault("difficulty", "hard");
        MODE = CONFIG.getOrDefault("mode", "default");
        ANALYTICS_ENABLED = CONFIG.getOrDefault("analytics_enabled", true);
        ANALYTICS_ENDPOINT = CONFIG.getOrDefault("analytics_endpoint", "https://us.i.posthog.com/capture/");
        ANALYTICS_ERROR_ENDPOINT = CONFIG.getOrDefault("analytics_error_endpoint", "https://us.i.posthog.com/i/v0/e/");
        ANALYTICS_PROJECT_TOKEN = CONFIG.getOrDefault("analytics_project_token", "phc_y6HcjBmwz7UEPQhaqYmJhdtHbh6VzRrv9h2MthHoLkdY");

        Path configDirectory = SimpleConfig.configDirectory();
        defaultValuesFile = configDirectory.resolve(DEFAULT_EMC_FILE_NAME);
        overridesFile = configDirectory.resolve(EMC_OVERRIDES_FILE_NAME);

        DefaultEmcValues.writeDefaultFile(defaultValuesFile);
        DefaultEmcValues.writeOverrideTemplateIfMissing(overridesFile);

        DEFAULT_EMC_VALUES = DefaultEmcValues.loadFromFile(defaultValuesFile);
        EMC_OVERRIDES = DefaultEmcValues.loadFromFile(overridesFile);
    }

    private static void ensureAnalyticsConfigEntries() {
        List<String> missingLines = configs.getConfigsList().stream()
            .filter(entry -> entry.getProperty().startsWith("analytics_"))
            .filter(entry -> !CONFIG.getConfig().containsKey(entry.getProperty()))
            .map(ModConfig::configLine)
            .toList();

        if (missingLines.isEmpty()) {
            return;
        }

        Path configFile = SimpleConfig.configDirectory().resolve(CONFIG_FILE_NAME + ".properties");
        try {
            String existing = Files.exists(configFile) ? Files.readString(configFile) : "";
            StringBuilder appended = new StringBuilder();
            if (!existing.isEmpty() && !existing.endsWith("\n")) {
                appended.append("\n");
            }
            for (String line : missingLines) {
                appended.append(line).append("\n");
            }
            Files.writeString(configFile, appended.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            DissolverEnhanced.LOGGER.warn("Could not append missing analytics config entries.", exception);
        }
    }

    private static String configLine(ConfigEntry<?> entry) {
        ConfigConstants constant = configConstant(entry);
        return entry.getProperty() + "=" + entry.getDefault() + " # "
            + constant.getComment() + " [default: " + entry.getDefault() + "]";
    }

    private static ConfigConstants configConstant(ConfigEntry<?> entry) {
        for (ConfigConstants constant : ConfigConstants.values()) {
            if (constant.asConfigEntry() == entry) {
                return constant;
            }
        }

        throw new IllegalArgumentException("Unknown config entry: " + entry.getProperty());
    }

    public static Path defaultValuesFile() {
        return defaultValuesFile;
    }

    public static Path overridesFile() {
        return overridesFile;
    }
}
