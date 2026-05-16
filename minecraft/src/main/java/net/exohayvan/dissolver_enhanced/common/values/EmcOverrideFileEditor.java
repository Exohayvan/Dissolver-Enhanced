package net.exohayvan.dissolver_enhanced.common.values;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcOverrideFileEditor {
    private static final String HEADER = String.join("\n",
        "# Dissolver Enhanced EMC overrides.",
        "# Put custom pack/server values here. These values are applied after default-emc-values.yaml.",
        "# Use null or 0 to remove a default value.",
        ""
    );

    private EmcOverrideFileEditor() {
    }

    public static BigInteger getItemValue(Path file, String itemId) {
        return DefaultEmcValues.loadFromFile(file).items().get(itemId);
    }

    public static void setItemValue(Path file, String itemId, BigInteger value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("EMC value must be greater than 0.");
        }

        EmcValueSet existingValues = DefaultEmcValues.loadFromFile(file);
        Map<String, BigInteger> items = new LinkedHashMap<>(existingValues.items());
        items.put(itemId, value);
        write(file, items, existingValues.tags());
    }

    public static boolean clearItemValue(Path file, String itemId) {
        EmcValueSet existingValues = DefaultEmcValues.loadFromFile(file);
        Map<String, BigInteger> items = new LinkedHashMap<>(existingValues.items());
        boolean removed = items.remove(itemId) != null;
        if (removed) {
            write(file, items, existingValues.tags());
        }

        return removed;
    }

    private static void write(Path file, Map<String, BigInteger> items, Map<String, BigInteger> tags) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, format(items, tags), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write EMC overrides to " + file, e);
        }
    }

    private static String format(Map<String, BigInteger> items, Map<String, BigInteger> tags) {
        StringBuilder builder = new StringBuilder(HEADER);
        builder.append("schema: 1\n");
        appendSection(builder, "items", items);
        appendSection(builder, "tags", tags);
        return builder.toString();
    }

    private static void appendSection(StringBuilder builder, String name, Map<String, BigInteger> values) {
        builder.append(name).append(":\n");
        values.forEach((key, value) -> {
            if (value != null) {
                builder.append("  ").append(key).append(": ").append(value).append("\n");
            }
        });
    }
}
