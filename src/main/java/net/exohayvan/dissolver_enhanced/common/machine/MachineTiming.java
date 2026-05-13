package net.exohayvan.dissolver_enhanced.common.machine;

public final class MachineTiming {
    public static final int TICKS_PER_SECOND = 20;

    private MachineTiming() {
    }

    public static int ticksForEmc(int emc, int ticksPerEmc) {
        if (emc <= 0) return Math.max(1, ticksPerEmc);

        long ticks = (long)emc * ticksPerEmc;
        return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)ticks;
    }

    public static int ticksPerEmc(int secondsPerEmc) {
        return TICKS_PER_SECOND * Math.max(1, secondsPerEmc);
    }
}
