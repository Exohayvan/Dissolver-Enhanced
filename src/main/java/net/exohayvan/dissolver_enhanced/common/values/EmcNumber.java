package net.exohayvan.dissolver_enhanced.common.values;

import java.math.BigInteger;
import java.util.Locale;

public final class EmcNumber {
    public static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger THOUSAND = BigInteger.valueOf(1_000);
    private static final BigInteger[] FORMAT_DIVISORS = {
        BigInteger.TEN.pow(30),
        BigInteger.TEN.pow(27),
        BigInteger.TEN.pow(24),
        BigInteger.TEN.pow(21),
        BigInteger.TEN.pow(18),
        BigInteger.TEN.pow(15),
        BigInteger.TEN.pow(12),
        BigInteger.TEN.pow(9),
        BigInteger.TEN.pow(6),
        BigInteger.TEN.pow(3)
    };
    private static final String[] FORMAT_SUFFIXES = {
        "dc",
        "no",
        "oc",
        "sp",
        "sx",
        "q",
        "t",
        "b",
        "m",
        "k"
    };

    private EmcNumber() {
    }

    public static BigInteger nonNegative(BigInteger value) {
        if (value == null || value.signum() < 0) return ZERO;
        return value;
    }

    public static BigInteger of(long value) {
        return nonNegative(BigInteger.valueOf(value));
    }

    public static BigInteger parse(String value) {
        if (value == null) return ZERO;

        String normalized = value.trim().replace(",", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return ZERO;

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

    public static int toIntSaturated(BigInteger value) {
        BigInteger nonNegative = nonNegative(value);
        if (nonNegative.compareTo(INT_MAX) > 0) return Integer.MAX_VALUE;
        return nonNegative.intValue();
    }

    public static String format(BigInteger value) {
        BigInteger nonNegative = nonNegative(value);
        if (nonNegative.compareTo(THOUSAND) < 0) return nonNegative.toString();

        for (int i = 0; i < FORMAT_DIVISORS.length; i++) {
            BigInteger divisor = FORMAT_DIVISORS[i];
            if (nonNegative.compareTo(divisor) >= 0) {
                return formatWithSuffix(nonNegative, divisor, FORMAT_SUFFIXES[i]);
            }
        }

        return nonNegative.toString();
    }

    private static String formatWithSuffix(BigInteger value, BigInteger divisor, String suffix) {
        BigInteger whole = value.divide(divisor);
        BigInteger remainder = value.remainder(divisor);
        BigInteger decimal = remainder.multiply(BigInteger.valueOf(100)).divide(divisor);
        return whole + "." + String.format(Locale.ROOT, "%02d", decimal.intValue()) + suffix;
    }
}
