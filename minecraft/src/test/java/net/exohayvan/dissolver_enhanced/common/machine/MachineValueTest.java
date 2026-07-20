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
    void rejectsMissingKeysAndNonPositiveEmc() {
        assertEquals(BigInteger.ZERO, MachineValue.scale(null, BigInteger.TEN, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale(" ", BigInteger.TEN, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale("minecraft:test", null, 1.0));
        assertEquals(BigInteger.ZERO, MachineValue.scale("minecraft:test", BigInteger.ZERO, 1.0));
    }
}
