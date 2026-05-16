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
        ParseState state = new ParseState();
        String[] lines = yaml.split("\\r?\\n");
        for (String rawLine : lines) {
            state.parseLine(rawLine);
        }

        Map<String, EmcValueOverride> immutableOverrides = new LinkedHashMap<>();
        state.overrides.forEach((key, value) -> immutableOverrides.put(key, new EmcValueOverride(value.items, value.tags)));

        return new EmcValueSet(state.schema, state.items, state.tags, immutableOverrides);
    }

    private static boolean isIgnoredLine(String rawLine) {
        return rawLine.isBlank() || rawLine.stripLeading().startsWith("#");
    }

    private static boolean isOverrideSection(Section section) {
        return section == Section.OVERRIDES || section == Section.OVERRIDE_ITEMS || section == Section.OVERRIDE_TAGS;
    }

    private static Section topLevelSection(String line) {
        if (line.equals("items:")) return Section.ITEMS;
        if (line.equals("tags:")) return Section.TAGS;
        if (line.equals("overrides:")) return Section.OVERRIDES;
        return Section.NONE;
    }

    private static Section overrideChildSection(String line) {
        if (line.equals("items:")) return Section.OVERRIDE_ITEMS;
        if (line.equals("tags:")) return Section.OVERRIDE_TAGS;
        return Section.OVERRIDES;
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

    private static final class ParseState {
        private int schema;
        private final Map<String, BigInteger> items = new LinkedHashMap<>();
        private final Map<String, BigInteger> tags = new LinkedHashMap<>();
        private final Map<String, MutableOverride> overrides = new LinkedHashMap<>();
        private Section section = Section.NONE;
        private String currentOverride;

        private void parseLine(String rawLine) {
            if (isIgnoredLine(rawLine)) return;

            int indent = countLeadingSpaces(rawLine);
            String line = rawLine.strip();

            if (indent == 0) {
                parseTopLevelLine(line);
            } else if (indent == 2) {
                parseSecondLevelLine(line);
            } else if (indent == 4 && isOverrideSection(section)) {
                parseOverrideChild(line);
            } else if (indent == 6) {
                parseOverrideValue(line);
            }
        }

        private void parseTopLevelLine(String line) {
            resetCurrentOverride();
            if (line.startsWith("schema:")) {
                schema = parseInteger(valuePart(line), "schema");
                section = Section.NONE;
                return;
            }

            section = topLevelSection(line);
        }

        private void parseSecondLevelLine(String line) {
            if (section == Section.ITEMS) {
                parseValueLine(line, items);
                return;
            }

            if (section == Section.TAGS) {
                parseValueLine(line, tags);
                return;
            }

            if (isOverrideSection(section)) {
                currentOverride = stripTrailingColon(line);
                overrides.computeIfAbsent(currentOverride, ignored -> new MutableOverride());
                section = Section.OVERRIDES;
            }
        }

        private void parseOverrideChild(String line) {
            if (currentOverride == null) return;
            section = overrideChildSection(line);
        }

        private void parseOverrideValue(String line) {
            if (currentOverride == null) return;

            MutableOverride override = overrides.get(currentOverride);
            if (section == Section.OVERRIDE_ITEMS) {
                parseValueLine(line, override.items);
            } else if (section == Section.OVERRIDE_TAGS) {
                parseValueLine(line, override.tags);
            }
        }

        private void resetCurrentOverride() {
            currentOverride = null;
        }
    }

    private static final class MutableOverride {
        private final Map<String, BigInteger> items = new LinkedHashMap<>();
        private final Map<String, BigInteger> tags = new LinkedHashMap<>();
    }
}
