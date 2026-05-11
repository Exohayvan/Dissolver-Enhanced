package net.exohayvan.dissolver_enhanced.common.values;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class DefaultEmcValues {
    private static final String DEFAULT_RESOURCE = "/emc-values/defaults.yaml";

    private DefaultEmcValues() {
    }

    public static EmcValueSet load() {
        try (InputStream stream = DefaultEmcValues.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing common EMC values resource: " + DEFAULT_RESOURCE);
            }

            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return EmcValueYamlParser.parse(yaml);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load common EMC values", e);
        }
    }
}
