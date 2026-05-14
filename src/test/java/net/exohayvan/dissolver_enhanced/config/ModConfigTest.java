package net.exohayvan.dissolver_enhanced.config;

import net.exohayvan.dissolver_enhanced.TestConstants;
import net.exohayvan.dissolver_enhanced.common.config.ConfigConstants;
import net.exohayvan.dissolver_enhanced.common.config.ConfigEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
class ModConfigTest {
    class VisibilityModifier
        extends ModConfig {

        ModConfigProvider configs() {
            return VisibilityModifier.configs;
        }

    }

    VisibilityModifier underTest;

    @BeforeEach
    void setUp() {
        underTest = new VisibilityModifier();

        flushConfig();
    }

    private void flushConfig() {
        //flush config
        VisibilityModifier.CONFIG = null;
        VisibilityModifier.MODE = null;
        VisibilityModifier.DIFFICULTY = null;
        VisibilityModifier.EMC_OVERRIDES = null;

        VisibilityModifier.CREATIVE_ITEMS = false;
        VisibilityModifier.EMC_ON_HUD = false;
        VisibilityModifier.PRIVATE_EMC = false;
        SimpleConfig.configDirSupplier = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR::get;
    }

    @Test
    void init_fresh() throws IOException {
        TestConstants.Configs.Directories directory = TestConstants.Configs.Directories.EMPTY;
        defaultsTest(directory, true);

        checkValues(directory);

    }

    @Test
    void init_existing_defaults() throws IOException {
        TestConstants.Configs.Directories directory = TestConstants.Configs.Directories.DEFAULT;
        defaultsTest(directory, false);

        checkValues(directory);

    }


    @Test
    void init_existing_inverts() throws IOException {
        TestConstants.Configs.Directories directory = TestConstants.Configs.Directories.INVERTED;
        defaultsTest(directory, false);

        checkValues(directory);
    }

    @Test
    void init_existing_emc_overrides() throws IOException {
        TestConstants.Configs.Directories directory = TestConstants.Configs.Directories.OVERRIDES;
        defaultsTest(directory, false);

        checkValues(directory);
    }

    private void checkValues(TestConstants.Configs.Directories directory) {
        switch (directory) {
            case DEFAULT:
            case EMPTY:

                isDefaults();

                assertThat(VisibilityModifier.EMC_OVERRIDES.items()).isEmpty();
                assertThat(VisibilityModifier.EMC_OVERRIDES.tags()).isEmpty();

                break;

            case OVERRIDES:

                isDefaults();

                assertThat(VisibilityModifier.EMC_OVERRIDES.items())
                    .containsEntry("test:case", BigInteger.valueOf(400))
                    .containsEntry("minecraft:dirt", BigInteger.valueOf(200))
                    .containsEntry("minecraft:cobblestone", BigInteger.valueOf(300));

                break;
            case INVERTED:

                assertThat(VisibilityModifier.MODE).isNotEqualTo(ConfigConstants.MODE
                    .asConfigEntry()
                    .getDefault());
                assertThat(VisibilityModifier.DIFFICULTY).isNotEqualTo(ConfigConstants.DIFFICULTY
                    .asConfigEntry()
                    .getDefault());
                assertThat(VisibilityModifier.CREATIVE_ITEMS).isNotEqualTo(ConfigConstants.CREATIVE_ITEMS
                    .asConfigEntry()
                    .getDefault());
                assertThat(VisibilityModifier.EMC_ON_HUD).isNotEqualTo(ConfigConstants.EMC_ON_HUD
                    .asConfigEntry()
                    .getDefault());
                assertThat(VisibilityModifier.PRIVATE_EMC).isNotEqualTo(ConfigConstants.PRIVATE_EMC
                    .asConfigEntry()
                    .getDefault());

                assertThat(VisibilityModifier.EMC_OVERRIDES.items()).isEmpty();
                assertThat(VisibilityModifier.EMC_OVERRIDES.tags()).isEmpty();

                break;

            default:
                throw new AssertionError("Forgot to define the values for directory=" + directory);
        }
    }

    private void isDefaults() {
        assertThat(VisibilityModifier.MODE).isEqualTo(ConfigConstants.MODE
            .asConfigEntry()
            .getDefault());
        assertThat(VisibilityModifier.DIFFICULTY).isEqualTo(ConfigConstants.DIFFICULTY
            .asConfigEntry()
            .getDefault());
        assertThat(VisibilityModifier.CREATIVE_ITEMS).isEqualTo(ConfigConstants.CREATIVE_ITEMS
            .asConfigEntry()
            .getDefault());
        assertThat(VisibilityModifier.EMC_ON_HUD).isEqualTo(ConfigConstants.EMC_ON_HUD
            .asConfigEntry()
            .getDefault());
        assertThat(VisibilityModifier.PRIVATE_EMC).isEqualTo(ConfigConstants.PRIVATE_EMC
            .asConfigEntry()
            .getDefault());
    }

    private void defaultsTest(
        TestConstants.Configs.Directories directory,
        boolean shouldDeleteExistingConfig
    ) throws IOException {
        setUpConfigDirectory(directory, shouldDeleteExistingConfig);

        ModConfigProvider expected = new ModConfigProvider();
        expected.addKeyValuePair(new ConfigEntry<>("emc_on_hud", false),
            "Display current EMC on HUD (top left corner)"
        );
        // WIP not added (Note: Turning this on will disable redstone integration.)

        expected.addKeyValuePair(new ConfigEntry<>("private_emc", false),
            "Should each player have their own EMC storage?"
        );
        expected.addKeyValuePair(new ConfigEntry<>("creative_items", false), "Should creative items have EMC?");

        expected.addKeyValuePair(
            new ConfigEntry<>("difficulty", "hard"),
            "easy | normal | hard - Changes crafting recipe for Dissolver block."
        );

        expected.addKeyValuePair(new ConfigEntry<>("mode", "default"),
            "default | skyblock - Changes some EMC values."
        );

        ModConfig.init();

        ModConfigProvider actual = underTest.configs();

        assertThat(actual.getConfigsList()).containsAll(expected.getConfigsList());

        verifyDefaultFile(directory);
    }

    private static void verifyDefaultFile(TestConstants.Configs.Directories directory) throws IOException {
        Configuration dirActual = getMyConfigLocation(directory);

        assertThat(Files.readAllLines(dirActual.config.toPath(), Charset.defaultCharset())).containsAll(
            directory.getExpectedConfig());
    }

    private static void setUpConfigDirectory(
        TestConstants.Configs.Directories directories,
        boolean shouldDelete
    ) {
        Configuration myConfigLocation = getMyConfigLocation(directories);

        Path path = myConfigLocation
            .file()
            .toPath();

        if (shouldDelete && myConfigLocation
            .config()
            .exists()) {
            assertThat(myConfigLocation
                .config()
                .delete())
                .as("Empty folder should be empty")
                .isTrue();
        }

        SimpleConfig.configDirSupplier = () -> path;
    }

    private static Configuration getMyConfigLocation(TestConstants.Configs.Directories directories) {
        File file = new File(directories.getValue());
        File config = new File(new File(file, "dissolver-enhanced"), TestConstants.Configs.FILE_NAME);
        Configuration myConfigLocation = new Configuration(file, config);
        return myConfigLocation;
    }

    private record Configuration(File file, File config) {}
}
