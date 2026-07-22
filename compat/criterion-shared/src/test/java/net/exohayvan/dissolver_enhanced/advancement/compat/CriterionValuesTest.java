package net.exohayvan.dissolver_enhanced.advancement.compat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionValuesTest {
    @Test
    void parsesCanonicalEmcFormats() {
        assertThat(CriterionValues.parse(null)).isEqualTo(BigInteger.ZERO);
        assertThat(CriterionValues.parse("   ")).isEqualTo(BigInteger.ZERO);
        assertThat(CriterionValues.parse(" 1,000 ")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValues.parse("1_000")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValues.parse("1k")).isEqualTo(BigInteger.valueOf(1_000));
        assertThat(CriterionValues.parse("2M")).isEqualTo(BigInteger.valueOf(2_000_000));
        assertThat(CriterionValues.parse("3b")).isEqualTo(BigInteger.valueOf(3_000_000_000L));
        assertThat(CriterionValues.parse("4t")).isEqualTo(BigInteger.TEN.pow(12).multiply(BigInteger.valueOf(4)));
        assertThat(CriterionValues.parse("5q")).isEqualTo(BigInteger.TEN.pow(15).multiply(BigInteger.valueOf(5)));
        assertThat(CriterionValues.parse("-1k")).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void rejectsMalformedValuesLikeCanonicalParser() {
        assertThatThrownBy(() -> CriterionValues.parse("not-emc")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> CriterionValues.parse("k")).isInstanceOf(NumberFormatException.class);
    }
}