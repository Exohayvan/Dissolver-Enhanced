package net.exohayvan.dissolver_enhanced.common.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class DefaultEmcValuesTest {
    @Test
    void loadsDefaultValues() {
        EmcValueSet values = DefaultEmcValues.load();

        assertEquals(1, values.schema());
        assertTrue(values.items().size() > 300);
        assertTrue(values.tags().size() > 40);
        assertEquals(BigInteger.valueOf(2), values.items().get("minecraft:cobblestone"));
        assertEquals(BigInteger.valueOf(16), values.tags().get("minecraft:logs"));
    }

    @Test
    void appliesSkyblockOverride() {
        EmcValueSet values = DefaultEmcValues.load().applyOverride("skyblock");

        assertEquals(BigInteger.valueOf(80), values.items().get("minecraft:dirt"));
        assertEquals(BigInteger.valueOf(8000), values.tags().get("minecraft:diamond_ores"));
    }
}
