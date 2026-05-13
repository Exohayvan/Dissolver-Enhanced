package net.exohayvan.dissolver_enhanced.common.values;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcValueYamlParser {
    private enum Section {
        NONE,
        ITEMS,
        TAGS,
        OVERRIDES,
        OVERRIDE_ITEMS,
        OVERRIDE_TAGS
    }

    private EmcValueYamlParser() {
    }

    public static EmcValueSet parse(String yaml) {
        int schema = 0;
        Map<String, BigInteger> items = new LinkedHashMap<>();
        Map<String, BigInteger> tags = new LinkedHashMap<>();
        Map<String, MutableOverride> overrides = new LinkedHashMap<>();

        Section section = Section.NONE;
        String currentOverride = null;

        String[] lines = yaml.split("\\r?\\n");
        for (String rawLine : lines) {
            if (rawLine.isBlank() || rawLine.stripLeading().startsWith("#")) {
                continue;
            }

            int indent = countLeadingSpaces(rawLine);
            String line = rawLine.strip();

            if (indent == 0) {
                if (line.startsWith("schema:")) {
                    schema = parseInteger(valuePart(line), "schema");
                    section = Section.NONE;
                    currentOverride = null;
                } else if (line.equals("items:")) {
                    section = Section.ITEMS;
                    currentOverride = null;
                } else if (line.equals("tags:")) {
                    section = Section.TAGS;
                    currentOverride = null;
                } else if (line.equals("overrides:")) {
                    section = Section.OVERRIDES;
                    currentOverride = null;
                }
                continue;
            }

            if (section == Section.ITEMS && indent == 2) {
                parseValueLine(line, items);
                continue;
            }

            if (section == Section.TAGS && indent == 2) {
                parseValueLine(line, tags);
                continue;
            }

            if ((section == Section.OVERRIDES || section == Section.OVERRIDE_ITEMS || section == Section.OVERRIDE_TAGS) && indent == 2) {
                currentOverride = stripTrailingColon(line);
                overrides.computeIfAbsent(currentOverride, ignored -> new MutableOverride());
                section = Section.OVERRIDES;
                continue;
            }

            if ((section == Section.OVERRIDES || section == Section.OVERRIDE_ITEMS || section == Section.OVERRIDE_TAGS) && indent == 4) {
                if (currentOverride == null) {
                    continue;
                }

                if (line.equals("items:")) {
                    section = Section.OVERRIDE_ITEMS;
                } else if (line.equals("tags:")) {
                    section = Section.OVERRIDE_TAGS;
                }
                continue;
            }

            if (section == Section.OVERRIDE_ITEMS && indent == 6 && currentOverride != null) {
                parseValueLine(line, overrides.get(currentOverride).items);
                continue;
            }

            if (section == Section.OVERRIDE_TAGS && indent == 6 && currentOverride != null) {
                parseValueLine(line, overrides.get(currentOverride).tags);
            }
        }

        Map<String, EmcValueOverride> immutableOverrides = new LinkedHashMap<>();
        overrides.forEach((key, value) -> immutableOverrides.put(key, new EmcValueOverride(value.items, value.tags)));

        return new EmcValueSet(schema, items, tags, immutableOverrides);
    }

    private static void parseValueLine(String line, Map<String, BigInteger> values) {
        int splitIndex = line.lastIndexOf(": ");
        if (splitIndex == -1) {
            return;
        }

        String key = line.substring(0, splitIndex).strip();
        String rawValue = line.substring(splitIndex + 2).strip();
        BigInteger value = parseNullableInteger(rawValue, key);
        values.put(key, value);
    }

    private static String valuePart(String line) {
        int splitIndex = line.indexOf(":");
        return splitIndex == -1 ? "" : line.substring(splitIndex + 1).strip();
    }

    private static int parseInteger(String value, String key) {
        BigInteger parsed = parseNullableInteger(value, key);
        if (parsed == null) {
            throw new IllegalArgumentException("Expected integer value for " + key);
        }

        return parsed.intValueExact();
    }

    private static BigInteger parseNullableInteger(String value, String key) {
        if (value.equals("null")) {
            return null;
        }

        try {
            return EmcNumber.parse(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for " + key + ": " + value, e);
        }
    }

    private static int countLeadingSpaces(String value) {
        int count = 0;
        while (count < value.length() && value.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String stripTrailingColon(String value) {
        if (!value.endsWith(":")) {
            return value;
        }

        return value.substring(0, value.length() - 1);
    }

    private static final class MutableOverride {
        private final Map<String, BigInteger> items = new LinkedHashMap<>();
        private final Map<String, BigInteger> tags = new LinkedHashMap<>();
    }
}
