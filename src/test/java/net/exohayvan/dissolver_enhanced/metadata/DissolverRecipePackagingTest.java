package net.exohayvan.dissolver_enhanced.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DissolverRecipePackagingTest {
    @Test
    void packagesTheDissolverRecipeUnlockAdvancement() throws Exception {
        String resourcePath = "/data/dissolver_enhanced/advancement/recipes/misc/dissolver_block.json";

        try (var stream = getClass().getResourceAsStream(resourcePath)) {
            assertThat(stream)
                .as("the generated resources include %s", resourcePath)
                .isNotNull();

            JsonObject advancement = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            assertThat(advancement
                .getAsJsonObject("rewards")
                .getAsJsonArray("recipes")
                .get(0)
                .getAsString())
                .isEqualTo("dissolver_enhanced:dissolver_block_recipe");
        }
    }
}
