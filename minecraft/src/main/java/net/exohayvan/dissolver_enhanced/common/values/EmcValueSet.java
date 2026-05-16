package net.exohayvan.dissolver_enhanced.common.values;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcValueSet {
    private final int schema;
    private final Map<String, BigInteger> items;
    private final Map<String, BigInteger> tags;
    private final Map<String, EmcValueOverride> overrides;

    public EmcValueSet(
        int schema,
        Map<String, BigInteger> items,
        Map<String, BigInteger> tags,
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

    public Map<String, BigInteger> items() {
        return items;
    }

    public Map<String, BigInteger> tags() {
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

        Map<String, BigInteger> mergedItems = new LinkedHashMap<>(items);
        override.items().forEach((key, value) -> applyOverrideValue(mergedItems, key, value));

        Map<String, BigInteger> mergedTags = new LinkedHashMap<>(tags);
        override.tags().forEach((key, value) -> applyOverrideValue(mergedTags, key, value));

        return new EmcValueSet(schema, mergedItems, mergedTags, overrides);
    }

    public EmcValueSet applyValues(EmcValueSet overrideValues) {
        if (overrideValues == null) {
            return this;
        }

        Map<String, BigInteger> mergedItems = new LinkedHashMap<>(items);
        overrideValues.items().forEach((key, value) -> applyOverrideValue(mergedItems, key, value));

        Map<String, BigInteger> mergedTags = new LinkedHashMap<>(tags);
        overrideValues.tags().forEach((key, value) -> applyOverrideValue(mergedTags, key, value));

        return new EmcValueSet(schema, mergedItems, mergedTags, overrides);
    }

    private static void applyOverrideValue(Map<String, BigInteger> values, String key, BigInteger value) {
        if (value == null) {
            values.remove(key);
            return;
        }

        values.put(key, value);
    }

    private static Map<String, BigInteger> immutableCopy(Map<String, BigInteger> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static Map<String, EmcValueOverride> immutableOverrideCopy(Map<String, EmcValueOverride> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
