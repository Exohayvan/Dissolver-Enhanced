package net.exohayvan.dissolver_enhanced.block.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MachineBlockEntitySupportTest {
    @Test
    void roundsConversionTimeUpAndClampsIt() {
        assertThat(MachineBlockEntitySupport.ticksForRate(10, 3, 20)).isEqualTo(67);
        assertThat(MachineBlockEntitySupport.ticksForRate(1, 100, 20)).isEqualTo(1);
        assertThat(MachineBlockEntitySupport.ticksForRate(0, 100, 20)).isEqualTo(20);
    }
}
