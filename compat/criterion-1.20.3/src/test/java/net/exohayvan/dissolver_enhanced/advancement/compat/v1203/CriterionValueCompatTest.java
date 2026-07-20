package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionValueCompatTest {
    @Test
    void parsesCanonicalEmcFormats() {
        assertThat(CriterionValueCompat.parse(null)).isEqualTo(BigInteger.ZERO);
        assertThat(CriterionValueCompat.parse("   ")).isEqualTo(BigInteger.ZERO);
        assertThat(CriterionValueCompat.parse(" 1,000 ")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValueCompat.parse("1_000")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValueCompat.parse("1k")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValueCompat.parse("2M")).isEqualTo(BigInteger.valueOf(2_000_000));
        assertThat(CriterionValueCompat.parse("3b")).isEqualTo(BigInteger.valueOf(3_000_000_000L));
        assertThat(CriterionValueCompat.parse("4t")).isEqualTo(BigInteger.TEN.pow(12).multiply(BigInteger.valueOf(4)));
        assertThat(CriterionValueCompat.parse("5q")).isEqualTo(BigInteger.TEN.pow(15).multiply(BigInteger.valueOf(5)));
        assertThat(CriterionValueCompat.parse("-1k")).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void rejectsMalformedValuesLikeCanonicalParser() {
        assertThatThrownBy(() -> CriterionValueCompat.parse("not-emc"))
            .isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> CriterionValueCompat.parse("k"))
            .isInstanceOf(NumberFormatException.class);
    }
}
