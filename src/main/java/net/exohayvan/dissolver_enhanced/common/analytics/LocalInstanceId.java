package net.exohayvan.dissolver_enhanced.common.analytics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class LocalInstanceId {
    private LocalInstanceId() {
    }

    public static String readOrCreate(Path file) throws IOException {
        if (Files.exists(file)) {
            String existing = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) {
                return existing;
            }
        }

        Files.createDirectories(file.getParent());
        String id = UUID.randomUUID().toString();
        Files.writeString(file, id + System.lineSeparator(), StandardCharsets.UTF_8);
        return id;
    }
}
