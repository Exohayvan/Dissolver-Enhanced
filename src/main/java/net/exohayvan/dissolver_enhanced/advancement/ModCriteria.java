package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ModCriteria {
    public static final LearnedItemCriterion LEARNED_ITEM = CriteriaTriggers.register(new LearnedItemCriterion());
    public static final EmcBalanceCriterion EMC_BALANCE = CriteriaTriggers.register(new EmcBalanceCriterion());
    public static final LearnedCountCriterion LEARNED_COUNT = CriteriaTriggers.register(new LearnedCountCriterion());
    public static final EmcOrbCriterion EMC_ORB = CriteriaTriggers.register(new EmcOrbCriterion());

    public static void init() {
    }

    public static void triggerLearnedItem(Player player, String itemId) {
        if (player instanceof ServerPlayer serverPlayer) {
            LEARNED_ITEM.trigger(serverPlayer, itemId);
            Map<String, Object> properties = achievementProperties("learned_item");
            properties.put("item_id", itemId);
            ModAnalytics.captureAchievementEarned("learned_item", properties);
        }
    }

    public static void triggerEmcBalance(Player player, BigInteger emc) {
        if (player instanceof ServerPlayer serverPlayer) {
            EMC_BALANCE.trigger(serverPlayer, emc);
            Map<String, Object> properties = achievementProperties("emc_balance");
            properties.put("emc_value", emc.toString());
            ModAnalytics.captureAchievementEarned("emc_balance", properties);
        }
    }

    public static void triggerLearnedCount(Player player, int learnedItems) {
        if (player instanceof ServerPlayer serverPlayer) {
            LEARNED_COUNT.trigger(serverPlayer, learnedItems);
            Map<String, Object> properties = achievementProperties("learned_count");
            properties.put("learned_items", learnedItems);
            ModAnalytics.captureAchievementEarned("learned_count", properties);
        }
    }

    public static void triggerEmcOrb(Player player, BigInteger emc, String action) {
        if (player instanceof ServerPlayer serverPlayer) {
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
