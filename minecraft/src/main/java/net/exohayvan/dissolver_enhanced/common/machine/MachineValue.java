package net.exohayvan.dissolver_enhanced.common.machine;

import java.math.BigDecimal;
import java.math.BigInteger;

final class MachineValue {
    private MachineValue() {
    }

    static BigInteger scale(String stackKey, BigInteger baseEmc, double durabilityPercent) {
        if (stackKey == null || stackKey.isBlank() || baseEmc == null || baseEmc.signum() <= 0) {
            return BigInteger.ZERO;
        }

        BigInteger result = new BigDecimal(baseEmc)
            .multiply(BigDecimal.valueOf(durabilityPercent))
            .toBigInteger();
        return result.signum() > 0 ? result : BigInteger.ONE;
    }
}
