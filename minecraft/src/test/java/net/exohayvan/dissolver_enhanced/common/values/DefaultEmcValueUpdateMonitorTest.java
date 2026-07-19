package net.exohayvan.dissolver_enhanced.common.values;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DefaultEmcValueUpdateMonitorTest {
    @Test
    void downloadsDefaultsFromTheCommonBranch() {
        assertEquals(
            "https://raw.githubusercontent.com/Exohayvan/Dissolver-Enhanced/refs/heads/common/minecraft/emc-values/defaults.yaml",
            DefaultEmcValueUpdateMonitor.DEFAULT_VALUES_URI.toString()
        );
    }
}
