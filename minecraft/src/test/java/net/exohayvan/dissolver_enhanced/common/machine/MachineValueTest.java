package net.exohayvan.dissolver_enhanced.common.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class MachineValueTest {
    @Test
    void scalesPositiveEmcWithFloorAndMinimumOne() {
        assertEquals(BigInteger.valueOf(50), MachineValue.scale("minecraft:test", BigInteger.valueOf(100), 0.5));
        assertEquals(BigInteger.ONE, MachineValue.scale("minecraft:test", BigInteger.ONE, 0.01));
    }

    @Test
    void publicMachineApisPreserveHelperResultsAndIntSaturation() {
        String key = "minecraft:test";
        BigInteger base = BigInteger.valueOf(100);

        assertEquals(MachineValue.scale(key, base, 0.5), CondenserLogic.getCondenseValue(key, base, 0.5));
        assertEquals(MachineValue.scale(key, base, 0.5), MaterializerLogic.getMaterializeValue(key, base, 0.5));
        assertEquals(50, CondenserLogic.getCondenseValue(key, 100, 0.5));
        assertEquals(50, MaterializerLogic.getMaterializeValue(key, 100, 0.5));
        assertEquals(Integer.MAX_VALUE, CondenserLogic.getCondenseValue(key, Integer.MAX_VALUE, 2.0));
        assertEquals(Integer.MAX_VALUE, MaterializerLogic.getMaterializeValue(key, Integer.MAX_VALUE, 2.0));

        BigInteger aboveIntRange = BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.TEN);
        BigInteger exactScaledValue = aboveIntRange.multiply(BigInteger.TWO);
        assertEquals(exactScaledValue, CondenserLogic.getCondenseValue(key, aboveIntRange, 2.0));
        assertEquals(exactScaledValue, MaterializerLogic.getMaterializeValue(key, aboveIntRange, 2.0));
    }

    @Test
    void rejectsMissingKeysAndNonPositiveEmc() {
        assertEquals(BigInteger.ZERO, MachineValue.scale(null, BigInteger.TEN, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale(" ", BigInteger.TEN, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale("minecraft:test", null, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale("minecraft:test", BigInteger.ZERO, 1.0));
    }
}
