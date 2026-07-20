package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;
import net.minecraft.util.math.Direction;

final class MachineBlockEntitySupport {
    private MachineBlockEntitySupport() {
    }

    static int ticksForRate(int emc, int emcPerSecond, int fallbackTicks) {
        if (emc <= 0) return fallbackTicks;

        long rate = Math.max(1, emcPerSecond);
        long ticks = ((long) emc * MachineTiming.TICKS_PER_SECOND + rate - 1L) / rate;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, ticks));
    }

    static int[] slotsFor(Direction direction, int[] topSlots, int[] bottomSlots, int[] sideSlots) {
        if (direction == Direction.UP) return topSlots;
        if (direction == Direction.DOWN) return bottomSlots;
        return sideSlots;
    }
}
