package net.exohayvan.dissolver_enhanced.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmcItemClassifierTest {
    @Test
    void extractsNamespaceAndItemNameFromComponentAwareKey() {
        String key = "minecraft:diamond_sword|damage=2";

        assertThat(EmcItemClassifier.namespace(key)).isEqualTo("minecraft");
        assertThat(EmcItemClassifier.itemName(key)).isEqualTo("diamond_sword");
    }

    @Test
    void recognizesCreativeOnlyItemsFromBaseIdentifier() {
        assertThat(EmcItemClassifier.isCreativeItem("minecraft:command_block[foo=bar]")).isTrue();
        assertThat(EmcItemClassifier.isCreativeItem("minecraft:diamond")).isFalse();
    }
}
