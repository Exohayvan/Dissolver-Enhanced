package net.exohayvan.dissolver_enhanced.common.machine;

public final class CondenserLogic {
    private CondenserLogic() {
    }

    public static int getCondenseValue(String stackKey, int baseEmc, double durabilityPercent) {
        if (stackKey == null || stackKey.isBlank() || baseEmc <= 0) return 0;

        return Math.max(1, (int)Math.floor(baseEmc * durabilityPercent));
    }

    public static int safeAdd(int current, int added) {
        long result = (long)current + added;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int)result);
    }
}
