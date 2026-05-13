package net.exohayvan.dissolver_enhanced.common.values;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DefaultEmcValues {
    private static final String DEFAULT_RESOURCE = "/emc-values/defaults.yaml";
    private static final String DEFAULT_FILE_HEADER = String.join("\n",
        "# Dissolver Enhanced default EMC values.",
        "# WARNING: This file is generated from the mod's bundled Common values and may be overwritten by updates.",
        "# Put pack/server tuning in emc-overrides.yaml instead.",
        ""
    );
    private static final String OVERRIDE_TEMPLATE = String.join("\n",
        "# Dissolver Enhanced EMC overrides.",
        "# Put custom pack/server values here. These values are applied after default-emc-values.yaml.",
        "# Use null or 0 to remove a default value.",
        "schema: 1",
        "items:",
        "  # minecraft:dirt: 1",
        "tags:",
        "  # minecraft:logs: 32",
        ""
    );

    private DefaultEmcValues() {
    }

    public static EmcValueSet load() {
        return EmcValueYamlParser.parse(loadBundledYaml());
    }

    public static EmcValueSet load(Path defaultValuesFile, Path overrideValuesFile) {
        writeDefaultFile(defaultValuesFile);
        writeOverrideTemplateIfMissing(overrideValuesFile);

        EmcValueSet defaultValues = loadFromFile(defaultValuesFile);
        EmcValueSet overrideValues = loadFromFile(overrideValuesFile);
        return defaultValues.applyValues(overrideValues);
    }

    public static EmcValueSet loadFromFile(Path file) {
        try {
            if (Files.notExists(file)) {
                return empty();
            }

            return EmcValueYamlParser.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load EMC values from " + file, e);
        }
    }

    public static void writeDefaultFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, DEFAULT_FILE_HEADER + loadBundledYaml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write default EMC values to " + file, e);
        }
    }

    public static void writeOverrideTemplateIfMissing(Path file) {
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                Files.writeString(file, OVERRIDE_TEMPLATE, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write EMC override template to " + file, e);
        }
    }

    private static EmcValueSet empty() {
        return new EmcValueSet(1, Map.of(), Map.of(), Map.of());
    }

    private static String loadBundledYaml() {
        try (InputStream stream = DefaultEmcValues.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing common EMC values resource: " + DEFAULT_RESOURCE);
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load common EMC values", e);
        }
    }
}
