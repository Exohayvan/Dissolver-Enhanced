package net.exohayvan.dissolver_enhanced.overlay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EMCOverlayDistTest {
    @Test
    void overlaySubscriberIsRestrictedToThePhysicalClient() throws IOException {
        byte[] bytecode;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
            "net/exohayvan/dissolver_enhanced/overlay/EMCOverlay.class"
        )) {
            assertThat(input).isNotNull();
            bytecode = input.readAllBytes();
        }

        String constants = new String(bytecode, StandardCharsets.ISO_8859_1);
        assertThat(constants).contains("net/minecraftforge/api/distmarker/Dist");
        assertThat(constants).contains("CLIENT");
    }
}
