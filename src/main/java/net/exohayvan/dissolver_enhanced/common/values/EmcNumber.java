package net.exohayvan.dissolver_enhanced.common.values;

import java.math.BigInteger;
import java.util.Locale;

public final class EmcNumber {
    public static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

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
        String raw = nonNegative(value).toString();
        StringBuilder formatted = new StringBuilder(raw.length() + raw.length() / 3);
        int firstGroup = raw.length() % 3;
        if (firstGroup == 0) firstGroup = 3;

        formatted.append(raw, 0, firstGroup);
        for (int i = firstGroup; i < raw.length(); i += 3) {
            formatted.append(',').append(raw, i, i + 3);
        }

        return formatted.toString();
    }
}
