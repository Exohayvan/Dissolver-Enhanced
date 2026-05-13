package net.exohayvan.dissolver_enhanced.common.values;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcValueSet {
    private final int schema;
    private final Map<String, Integer> items;
    private final Map<String, Integer> tags;
    private final Map<String, EmcValueOverride> overrides;

    public EmcValueSet(
        int schema,
        Map<String, Integer> items,
        Map<String, Integer> tags,
        Map<String, EmcValueOverride> overrides
    ) {
        this.schema = schema;
        this.items = immutableCopy(items);
        this.tags = immutableCopy(tags);
        this.overrides = immutableOverrideCopy(overrides);
    }

    public int schema() {
        return schema;
    }

    public Map<String, Integer> items() {
        return items;
    }

    public Map<String, Integer> tags() {
        return tags;
    }

    public Map<String, EmcValueOverride> overrides() {
        return overrides;
    }

    public EmcValueSet applyOverride(String overrideName) {
        EmcValueOverride override = overrides.get(overrideName);
        if (override == null) {
            return this;
        }

        Map<String, Integer> mergedItems = new LinkedHashMap<>(items);
        override.items().forEach((key, value) -> applyOverrideValue(mergedItems, key, value));

        Map<String, Integer> mergedTags = new LinkedHashMap<>(tags);
        override.tags().forEach((key, value) -> applyOverrideValue(mergedTags, key, value));

        return new EmcValueSet(schema, mergedItems, mergedTags, overrides);
    }

    public EmcValueSet applyValues(EmcValueSet overrideValues) {
        if (overrideValues == null) {
            return this;
        }

        Map<String, Integer> mergedItems = new LinkedHashMap<>(items);
        overrideValues.items().forEach((key, value) -> applyOverrideValue(mergedItems, key, value));

        Map<String, Integer> mergedTags = new LinkedHashMap<>(tags);
        overrideValues.tags().forEach((key, value) -> applyOverrideValue(mergedTags, key, value));

        return new EmcValueSet(schema, mergedItems, mergedTags, overrides);
    }

    private static void applyOverrideValue(Map<String, Integer> values, String key, Integer value) {
        if (value == null) {
            values.remove(key);
            return;
        }

        values.put(key, value);
    }

    private static Map<String, Integer> immutableCopy(Map<String, Integer> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static Map<String, EmcValueOverride> immutableOverrideCopy(Map<String, EmcValueOverride> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
