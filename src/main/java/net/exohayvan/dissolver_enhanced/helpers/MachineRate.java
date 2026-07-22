package net.exohayvan.dissolver_enhanced.helpers;

import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;

public final class MachineRate {
    private MachineRate() {
    }

    public static int ticksForRate(int emc, int emcPerSecond, int fallback) {
        if (emc <= 0) return fallback;

        int safeRate = Math.max(1, emcPerSecond);
        long ticks = ((long) emc * MachineTiming.TICKS_PER_SECOND + safeRate - 1L) / safeRate;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, ticks));
    }
}