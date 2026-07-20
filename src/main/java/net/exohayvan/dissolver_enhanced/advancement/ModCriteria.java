package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModCriteria {
    public static final Object LEARNED_ITEM = CriterionCompat.createAndRegister(
        "dissolver_enhanced:learned_item", "LearnedItemCriterion"
    );
    public static final Object EMC_BALANCE = CriterionCompat.createAndRegister(
        "dissolver_enhanced:emc_balance", "EmcBalanceCriterion"
    );
    public static final Object LEARNED_COUNT = CriterionCompat.createAndRegister(
        "dissolver_enhanced:learned_count", "LearnedCountCriterion"
    );
    public static final Object EMC_ORB = CriterionCompat.createAndRegister(
        "dissolver_enhanced:emc_orb", "EmcOrbCriterion"
    );

    public static void init(IEventBus eventBus) {
        CriterionCompat.registerDeferred(eventBus);
    }

    public static void triggerLearnedItem(Player player, String itemId) {
        if (player instanceof ServerPlayer serverPlayer) {
            CriterionCompat.trigger(LEARNED_ITEM, serverPlayer, itemId);
            Map<String, Object> properties = achievementProperties("learned_item");
            properties.put("item_id", itemId);
            ModAnalytics.captureAchievementEarned("learned_item", properties);
        }
    }

    public static void triggerEmcBalance(Player player, BigInteger emc) {
        if (player instanceof ServerPlayer serverPlayer) {
            CriterionCompat.trigger(EMC_BALANCE, serverPlayer, emc);
            Map<String, Object> properties = achievementProperties("emc_balance");
            properties.put("emc_value", emc.toString());
            ModAnalytics.captureAchievementEarned("emc_balance", properties);
        }
    }

    public static void triggerLearnedCount(Player player, int learnedItems) {
        if (player instanceof ServerPlayer serverPlayer) {
            CriterionCompat.trigger(LEARNED_COUNT, serverPlayer, learnedItems);
            Map<String, Object> properties = achievementProperties("learned_count");
            properties.put("learned_items", learnedItems);
            ModAnalytics.captureAchievementEarned("learned_count", properties);
        }
    }

    public static void triggerEmcOrb(Player player, BigInteger emc, String action) {
        if (player instanceof ServerPlayer serverPlayer) {
            CriterionCompat.trigger(EMC_ORB, serverPlayer, emc, action);
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
