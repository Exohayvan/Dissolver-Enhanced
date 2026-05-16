package net.exohayvan.dissolver_enhanced.common.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class EmcNumberTest {
    @Test
    void formatsCompactDisplayValues() {
        assertEquals("999", EmcNumber.format(BigInteger.valueOf(999)));
        assertEquals("1.00k", EmcNumber.format(BigInteger.valueOf(1_000)));
        assertEquals("1.50k", EmcNumber.format(BigInteger.valueOf(1_500)));
        assertEquals("20.00m", EmcNumber.format(BigInteger.valueOf(20_000_000)));
        assertEquals("1.25b", EmcNumber.format(BigInteger.valueOf(1_250_000_000)));
        assertEquals("1.00q", EmcNumber.format(BigInteger.TEN.pow(15)));
        assertEquals("1.00sp", EmcNumber.format(BigInteger.TEN.pow(21)));
    }
}
