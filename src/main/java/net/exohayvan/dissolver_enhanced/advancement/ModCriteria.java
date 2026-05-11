package net.exohayvan.dissolver_enhanced.advancement;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModCriteria {
    public static final LearnedItemCriterion LEARNED_ITEM = Criteria.register(
        DissolverEnhanced.MOD_ID + ":learned_item",
        new LearnedItemCriterion()
    );

    public static void init() {
    }

    public static void triggerLearnedItem(PlayerEntity player, String itemId) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LEARNED_ITEM.trigger(serverPlayer, itemId);
        }
    }
}
