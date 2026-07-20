package net.exohayvan.dissolver_enhanced.advancement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CriterionCompatTest {
    @Test
    void modCriteriaAvoidsVersionSpecificCriteriaTriggersRegisterMethod() throws IOException {
        byte[] bytecode;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
            "net/exohayvan/dissolver_enhanced/advancement/ModCriteria.class"
        )) {
            assertThat(input).isNotNull();
            bytecode = input.readAllBytes();
        }

        String constants = new String(bytecode, StandardCharsets.ISO_8859_1);
        assertThat(constants).doesNotContain("net/minecraft/advancements/CriteriaTriggers");
    }
}
