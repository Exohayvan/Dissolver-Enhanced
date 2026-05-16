package net.exohayvan.dissolver_enhanced.common.machine;

import java.math.BigInteger;
import java.math.BigDecimal;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public final class MaterializerLogic {
    private MaterializerLogic() {
    }

    public static boolean canOutput(int storedEmc, int targetValue) {
        return canOutput(BigInteger.valueOf(storedEmc), BigInteger.valueOf(targetValue));
    }

    public static boolean canOutput(BigInteger storedEmc, BigInteger targetValue) {
        BigInteger stored = EmcNumber.nonNegative(storedEmc);
        BigInteger target = EmcNumber.nonNegative(targetValue);
        return target.signum() > 0 && stored.compareTo(target) >= 0;
    }

    public static int absorbInput(int storedEmc, int inputValue) {
        return EmcNumber.toIntSaturated(absorbInput(BigInteger.valueOf(storedEmc), BigInteger.valueOf(inputValue)));
    }

    public static BigInteger absorbInput(BigInteger storedEmc, BigInteger inputValue) {
        if (inputValue == null || inputValue.signum() <= 0) return EmcNumber.nonNegative(storedEmc);

        return EmcNumber.nonNegative(storedEmc).add(inputValue);
    }

    public static int spendForOutput(int storedEmc, int targetValue) {
        return EmcNumber.toIntSaturated(spendForOutput(BigInteger.valueOf(storedEmc), BigInteger.valueOf(targetValue)));
    }

    public static BigInteger spendForOutput(BigInteger storedEmc, BigInteger targetValue) {
        if (!canOutput(storedEmc, targetValue)) return EmcNumber.nonNegative(storedEmc);

        return EmcNumber.nonNegative(storedEmc).subtract(EmcNumber.nonNegative(targetValue));
    }

    public static int getMaterializeValue(String stackKey, int baseEmc, double durabilityPercent) {
        return EmcNumber.toIntSaturated(getMaterializeValue(stackKey, BigInteger.valueOf(baseEmc), durabilityPercent));
    }

    public static BigInteger getMaterializeValue(String stackKey, BigInteger baseEmc, double durabilityPercent) {
        if (stackKey == null || stackKey.isBlank() || baseEmc == null || baseEmc.signum() <= 0) return BigInteger.ZERO;

        BigInteger result = new BigDecimal(baseEmc)
            .multiply(BigDecimal.valueOf(durabilityPercent))
            .toBigInteger();
        return result.signum() > 0 ? result : BigInteger.ONE;
    }
}
