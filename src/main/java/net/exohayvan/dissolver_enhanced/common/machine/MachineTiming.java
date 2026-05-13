package net.exohayvan.dissolver_enhanced.common.machine;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public final class MachineTiming {
    public static final int TICKS_PER_SECOND = 20;

    private MachineTiming() {
    }

    public static int ticksForEmc(int emc, int ticksPerEmc) {
        return EmcNumber.toIntSaturated(ticksForEmc(BigInteger.valueOf(emc), ticksPerEmc));
    }

    public static BigInteger ticksForEmc(BigInteger emc, int ticksPerEmc) {
        if (emc == null || emc.signum() <= 0) return BigInteger.valueOf(Math.max(1, ticksPerEmc));

        return emc.multiply(BigInteger.valueOf(ticksPerEmc));
    }

    public static int ticksPerEmc(int secondsPerEmc) {
        return TICKS_PER_SECOND * Math.max(1, secondsPerEmc);
    }
}
