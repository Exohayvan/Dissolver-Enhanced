package net.exohayvan.dissolver_enhanced.helpers;

import java.util.List;

import net.exohayvan.dissolver_enhanced.config.ModConfig;

public final class EmcItemClassifier {
    private static final List<String> CREATIVE_ITEM_MARKERS = List.of(
        "spawn_egg",
        "command_block",
        "bedrock",
        "barrier",
        "structure_block",
        "jigsaw",
        "spawner",
        "vault",
        "end_portal_frame",
        "budding_amethyst",
        "reinforced_deepslate"
    );

    private EmcItemClassifier() {
    }

    public static String baseItemId(String itemId) {
        return EMCKey.baseItemId(itemId);
    }

    public static String namespace(String itemId) {
        String baseItemId = baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? "unknown" : baseItemId.substring(0, namespaceEnd);
    }

    public static String itemName(String itemId) {
        String baseItemId = baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? baseItemId : baseItemId.substring(namespaceEnd + 1);
    }

    public static boolean isCreativeItem(String itemId) {
        String baseItemId = baseItemId(itemId);
        return CREATIVE_ITEM_MARKERS.stream().anyMatch(baseItemId::contains);
    }

    public static String rejectionReason(String itemId) {
        if (isCreativeItem(itemId) && !ModConfig.CREATIVE_ITEMS) {
            return "creative_disabled";
        }
        return "no_emc";
    }
}
