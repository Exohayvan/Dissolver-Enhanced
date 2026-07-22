package net.exohayvan.dissolver_enhanced;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ForgeModMetadataTest {
    @Test
    void supportsExactlyThePublishedForgeMinecraft20Versions() throws Exception {
        VersionRange range = VersionRange.createFromVersionSpec(properties().getProperty("minecraft_version_range"));

        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.1"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.2"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.3"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.4"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.5"))).isFalse();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.6"))).isFalse();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.20.7"))).isFalse();
        assertThat(range.containsVersion(new DefaultArtifactVersion("1.21"))).isFalse();
    }

    @Test
    void supportsEveryRequiredForgeLoaderGeneration() throws Exception {
        VersionRange range = VersionRange.createFromVersionSpec(properties().getProperty("loader_version_range"));

        assertThat(range.containsVersion(new DefaultArtifactVersion("46.0.14"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("47.4.21"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("48.1.0"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("49.2.8"))).isTrue();
        assertThat(range.containsVersion(new DefaultArtifactVersion("50.2.9"))).isTrue();
    }

    private static Properties properties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("gradle.properties"))) {
            properties.load(input);
        }
        return properties;
    }
}
