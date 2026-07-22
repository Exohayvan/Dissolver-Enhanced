package net.exohayvan.dissolver_enhanced.advancement.compat;

import java.math.BigInteger;
import java.util.Locale;

import com.google.gson.JsonObject;

public final class CriterionValues {
    private CriterionValues() {
    }

    public static String baseItemId(String key) {
        int componentIndex = key.indexOf('|');
        return componentIndex == -1 ? key : key.substring(0, componentIndex);
    }

    public static BigInteger nonNegative(BigInteger value) {
        return value == null || value.signum() < 0 ? BigInteger.ZERO : value;
    }

    public static BigInteger parse(String value) {
        if (value == null) return BigInteger.ZERO;
        String normalized = value.trim().replace(",", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return BigInteger.ZERO;

        int exponent = switch (normalized.charAt(normalized.length() - 1)) {
            case 'k' -> 3;
            case 'm' -> 6;
            case 'b' -> 9;
            case 't' -> 12;
            case 'q' -> 15;
            default -> 0;
        };
        String numeric = exponent == 0 ? normalized : normalized.substring(0, normalized.length() - 1);
        return nonNegative(new BigInteger(numeric).multiply(BigInteger.TEN.pow(exponent)));
    }

    public static String minimumEmc(JsonObject json) {
        return json.has("min_emc") ? json.get("min_emc").getAsString() : "0";
    }

    public static boolean meetsMinimum(BigInteger value, String minimum) {
        return nonNegative(value).compareTo(parse(minimum)) >= 0;
    }

    public static String optionalItem(JsonObject json) {
        return json.has("item") ? json.get("item").getAsString() : null;
    }

    public static Boolean optionalExternalNamespace(JsonObject json) {
        return json.has("external_namespace") ? json.get("external_namespace").getAsBoolean() : null;
    }

    public static boolean matchesLearnedItem(String expectedItem, Boolean externalNamespace, String itemId) {
        if (expectedItem != null && !expectedItem.equals(itemId)) return false;
        if (externalNamespace == null) return true;
        int namespaceEnd = itemId.indexOf(':');
        boolean external = namespaceEnd > 0 && !"minecraft".equals(itemId.substring(0, namespaceEnd));
        return externalNamespace == external;
    }
}