package net.exohayvan.dissolver_enhanced.common.values;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmcValueOverride {
    private final Map<String, BigInteger> items;
    private final Map<String, BigInteger> tags;

    public EmcValueOverride(Map<String, BigInteger> items, Map<String, BigInteger> tags) {
        this.items = immutableCopy(items);
        this.tags = immutableCopy(tags);
    }

    public Map<String, BigInteger> items() {
        return items;
    }

    public Map<String, BigInteger> tags() {
        return tags;
    }

    private static Map<String, BigInteger> immutableCopy(Map<String, BigInteger> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
