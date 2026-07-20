package net.exohayvan.dissolver_enhanced.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

class FabricMetadataCompatibilityTest {
    private static final Pattern MINECRAFT_DEPENDENCY = Pattern.compile(
        "\\\"minecraft\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );

    @Test
    void acceptsEveryMinecraft26Point1ReleaseOnly() throws Exception {
        VersionPredicate minecraftVersions = VersionPredicate.parse(readMinecraftDependency());

        assertThat(minecraftVersions.test(SemanticVersion.parse("26.1"))).isTrue();
        assertThat(minecraftVersions.test(SemanticVersion.parse("26.1.1"))).isTrue();
        assertThat(minecraftVersions.test(SemanticVersion.parse("26.1.2"))).isTrue();
        assertThat(minecraftVersions.test(SemanticVersion.parse("26.1.999"))).isTrue();

        assertThat(minecraftVersions.test(SemanticVersion.parse("26.0.9"))).isFalse();
        assertThat(minecraftVersions.test(SemanticVersion.parse("26.2"))).isFalse();
        assertThat(minecraftVersions.test(SemanticVersion.parse("27.0"))).isFalse();
    }

    private static String readMinecraftDependency() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        Matcher matcher = MINECRAFT_DEPENDENCY.matcher(metadata);
        assertThat(matcher.find()).as("fabric.mod.json declares a Minecraft dependency").isTrue();
        return matcher.group(1);
    }
}
