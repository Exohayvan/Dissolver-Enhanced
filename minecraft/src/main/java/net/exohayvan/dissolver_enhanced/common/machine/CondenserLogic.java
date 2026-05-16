package net.exohayvan.dissolver_enhanced.common.machine;

import java.math.BigInteger;
import java.math.BigDecimal;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public final class CondenserLogic {
    private CondenserLogic() {
    }

    public static int getCondenseValue(String stackKey, int baseEmc, double durabilityPercent) {
        return EmcNumber.toIntSaturated(getCondenseValue(stackKey, BigInteger.valueOf(baseEmc), durabilityPercent));
    }

    public static BigInteger getCondenseValue(String stackKey, BigInteger baseEmc, double durabilityPercent) {
        if (stackKey == null || stackKey.isBlank() || baseEmc == null || baseEmc.signum() <= 0) return BigInteger.ZERO;

        BigInteger result = new BigDecimal(baseEmc)
            .multiply(BigDecimal.valueOf(durabilityPercent))
            .toBigInteger();
        return result.signum() > 0 ? result : BigInteger.ONE;
    }

    public static int safeAdd(int current, int added) {
        return EmcNumber.toIntSaturated(safeAdd(BigInteger.valueOf(current), BigInteger.valueOf(added)));
    }

    public static BigInteger safeAdd(BigInteger current, BigInteger added) {
        return EmcNumber.nonNegative(EmcNumber.nonNegative(current).add(EmcNumber.nonNegative(added)));
    }
}
