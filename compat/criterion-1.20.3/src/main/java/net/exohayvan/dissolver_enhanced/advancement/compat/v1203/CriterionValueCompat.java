package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.math.BigInteger;
import java.util.Locale;

final class CriterionValueCompat {
    private CriterionValueCompat() {
    }

    static String baseItemId(String key) {
        int componentIndex = key.indexOf("|");
        return componentIndex == -1 ? key : key.substring(0, componentIndex);
    }

    static BigInteger nonNegative(BigInteger value) {
        return value == null || value.signum() < 0 ? BigInteger.ZERO : value;
    }

    static BigInteger parse(String value) {
        if (value == null) return BigInteger.ZERO;

        String normalized = value.trim().replace(",", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return BigInteger.ZERO;

        BigInteger multiplier = BigInteger.ONE;
        String numeric = normalized;
        if (normalized.endsWith("k")) {
            multiplier = BigInteger.TEN.pow(3);
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("m")) {
            multiplier = BigInteger.TEN.pow(6);
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("b")) {
            multiplier = BigInteger.TEN.pow(9);
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("t")) {
            multiplier = BigInteger.TEN.pow(12);
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("q")) {
            multiplier = BigInteger.TEN.pow(15);
            numeric = normalized.substring(0, normalized.length() - 1);
        }
        return nonNegative(new BigInteger(numeric).multiply(multiplier));
    }
}
