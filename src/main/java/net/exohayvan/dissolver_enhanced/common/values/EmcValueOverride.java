package net.exohayvan.dissolver_enhanced.common.values;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcValueOverride {
    private final Map<String, Integer> items;
    private final Map<String, Integer> tags;

    public EmcValueOverride(Map<String, Integer> items, Map<String, Integer> tags) {
        this.items = immutableCopy(items);
        this.tags = immutableCopy(tags);
    }

    public Map<String, Integer> items() {
        return items;
    }

    public Map<String, Integer> tags() {
        return tags;
    }

    private static Map<String, Integer> immutableCopy(Map<String, Integer> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
