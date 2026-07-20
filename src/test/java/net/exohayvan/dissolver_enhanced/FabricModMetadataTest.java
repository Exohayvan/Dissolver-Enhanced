package net.exohayvan.dissolver_enhanced;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FabricModMetadataTest {
    @Test
    void supportsEveryMinecraft26PointOnePatchVersion() throws IOException, VersionParsingException {
        JsonObject metadata;
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/fabric.mod.json"))) {
            metadata = JsonParser.parseReader(reader).getAsJsonObject();
        }
        String requirement = metadata
            .getAsJsonObject("depends")
            .get("minecraft")
            .getAsString();
        VersionPredicate predicate = VersionPredicate.parse(requirement);

        assertThat(predicate.test(Version.parse("26.1"))).isTrue();
        assertThat(predicate.test(Version.parse("26.1.1"))).isTrue();
        assertThat(predicate.test(Version.parse("26.1.2"))).isTrue();
        assertThat(predicate.test(Version.parse("26.2"))).isFalse();
    }
}
