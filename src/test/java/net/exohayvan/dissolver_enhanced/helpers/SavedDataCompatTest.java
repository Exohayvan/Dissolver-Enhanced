package net.exohayvan.dissolver_enhanced.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SavedDataCompatTest {
    @Test
    void selectsOnlyExistingStateLookupMethods() {
        assertThat(SavedDataCompat.isExistingLookupName("get")).isTrue();
        assertThat(SavedDataCompat.isExistingLookupName("m_164858_")).isTrue();
        assertThat(SavedDataCompat.isExistingLookupName("computeIfAbsent")).isFalse();
        assertThat(SavedDataCompat.isExistingLookupName("m_164861_")).isFalse();
    }
}
