package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModCriteria {
    public static final LearnedItemCriterion LEARNED_ITEM = Criteria.register(
        DissolverEnhanced.MOD_ID + ":learned_item",
        new LearnedItemCriterion()
    );
    public static final EmcBalanceCriterion EMC_BALANCE = Criteria.register(
        DissolverEnhanced.MOD_ID + ":emc_balance",
        new EmcBalanceCriterion()
    );
    public static final LearnedCountCriterion LEARNED_COUNT = Criteria.register(
        DissolverEnhanced.MOD_ID + ":learned_count",
        new LearnedCountCriterion()
    );
    public static final EmcOrbCriterion EMC_ORB = Criteria.register(
        DissolverEnhanced.MOD_ID + ":emc_orb",
        new EmcOrbCriterion()
    );

    public static void init() {
    }

    public static void triggerLearnedItem(PlayerEntity player, String itemId) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LEARNED_ITEM.trigger(serverPlayer, itemId);
            Map<String, Object> properties = achievementProperties("learned_item");
            properties.put("item_id", itemId);
            ModAnalytics.captureAchievementEarned("learned_item", properties);
        }
    }

    public static void triggerEmcBalance(PlayerEntity player, BigInteger emc) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EMC_BALANCE.trigger(serverPlayer, emc);
            Map<String, Object> properties = achievementProperties("emc_balance");
            properties.put("emc_value", emc.toString());
            ModAnalytics.captureAchievementEarned("emc_balance", properties);
        }
    }

    public static void triggerLearnedCount(PlayerEntity player, int learnedItems) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LEARNED_COUNT.trigger(serverPlayer, learnedItems);
            Map<String, Object> properties = achievementProperties("learned_count");
            properties.put("learned_items", learnedItems);
            ModAnalytics.captureAchievementEarned("learned_count", properties);
        }
    }

    public static void triggerEmcOrb(PlayerEntity player, BigInteger emc, String action) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EMC_ORB.trigger(serverPlayer, emc, action);
            Map<String, Object> properties = achievementProperties("emc_orb");
            properties.put("emc_value", emc.toString());
            properties.put("action", action);
            ModAnalytics.captureAchievementEarned("emc_orb", properties);
        }
    }

    private static Map<String, Object> achievementProperties(String criterion) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("criterion", criterion);
        return properties;
    }
}
