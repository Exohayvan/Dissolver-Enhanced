package net.exohayvan.dissolver_enhanced.config;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValues;
import net.exohayvan.dissolver_enhanced.common.values.EmcValueSet;
import net.exohayvan.dissolver_enhanced.config.model.ConfigConstants;

import java.nio.file.Path;

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
    public static EmcValueSet DEFAULT_EMC_VALUES;
    public static EmcValueSet EMC_OVERRIDES;

    public static void init() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(CONFIG_FILE_NAME).provider(configs).request();

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

        Path configDirectory = SimpleConfig.configDirectory();
        Path defaultValuesFile = configDirectory.resolve(DEFAULT_EMC_FILE_NAME);
        Path overridesFile = configDirectory.resolve(EMC_OVERRIDES_FILE_NAME);

        DefaultEmcValues.writeDefaultFile(defaultValuesFile);
        DefaultEmcValues.writeOverrideTemplateIfMissing(overridesFile);

        DEFAULT_EMC_VALUES = DefaultEmcValues.loadFromFile(defaultValuesFile);
        EMC_OVERRIDES = DefaultEmcValues.loadFromFile(overridesFile);
    }
}
