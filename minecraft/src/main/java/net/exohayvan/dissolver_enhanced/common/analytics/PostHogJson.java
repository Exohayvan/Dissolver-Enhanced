package net.exohayvan.dissolver_enhanced.common.analytics;

import java.util.Iterator;
import java.util.Map;

final class PostHogJson {
    private PostHogJson() {
    }

    static String toJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJsonValue(builder, value);
        return builder.toString();
    }

    private static void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendJsonObject(builder, map);
        } else if (value instanceof Iterable<?> iterable) {
            appendJsonArray(builder, iterable);
        } else {
            builder.append('"').append(jsonEscape(String.valueOf(value))).append('"');
        }
    }

    private static void appendJsonObject(StringBuilder builder, Map<?, ?> values) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(jsonEscape(String.valueOf(entry.getKey()))).append("\":");
            appendJsonValue(builder, entry.getValue());
        }
        builder.append('}');
    }

    private static void appendJsonArray(StringBuilder builder, Iterable<?> values) {
        builder.append('[');
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
            appendJsonValue(builder, iterator.next());
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
        builder.append(']');
    }

    private static String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }
}
