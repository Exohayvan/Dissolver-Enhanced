package net.exohayvan.dissolver_enhanced.common.machine;

public final class MaterializerLogic {
    private MaterializerLogic() {
    }

    public static boolean canOutput(int storedEmc, int targetValue) {
        return targetValue > 0 && storedEmc >= targetValue;
    }

    public static int absorbInput(int storedEmc, int inputValue) {
        if (inputValue <= 0) return storedEmc;

        long result = (long)storedEmc + inputValue;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int)result);
    }

    public static int spendForOutput(int storedEmc, int targetValue) {
        if (!canOutput(storedEmc, targetValue)) return storedEmc;

        return storedEmc - targetValue;
    }

    public static int getMaterializeValue(String stackKey, int baseEmc, double durabilityPercent) {
        if (stackKey == null || stackKey.isBlank() || baseEmc <= 0) return 0;

        return Math.max(1, (int)Math.floor(baseEmc * durabilityPercent));
    }
}
