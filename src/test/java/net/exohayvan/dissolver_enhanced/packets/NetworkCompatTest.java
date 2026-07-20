package net.exohayvan.dissolver_enhanced.packets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkCompatTest {
    @Test
    void packetEntryPointsDoNotLinkOneForgeNetworkingGeneration() throws IOException {
        assertClassDoesNotContain("net/exohayvan/dissolver_enhanced/packets/Packets.class", "net/minecraftforge/network/NetworkRegistry");
        assertClassDoesNotContain("net/exohayvan/dissolver_enhanced/packets/Packets.class", "net/minecraftforge/network/simple/SimpleChannel");
        assertClassDoesNotContain("net/exohayvan/dissolver_enhanced/packets/clientbound/EMCValuesPayload.class", "net/minecraftforge/network/NetworkEvent");
        assertClassDoesNotContain("net/exohayvan/dissolver_enhanced/packets/clientbound/PlayerDataPayload.class", "net/minecraftforge/network/NetworkEvent");
        assertClassDoesNotContain("net/exohayvan/dissolver_enhanced/packets/serverbound/ClientPayload.class", "net/minecraftforge/network/NetworkEvent");
    }

    private void assertClassDoesNotContain(String resource, String symbol) throws IOException {
        byte[] bytecode;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            bytecode = input.readAllBytes();
        }
        assertThat(new String(bytecode, StandardCharsets.ISO_8859_1)).doesNotContain(symbol);
    }
}
