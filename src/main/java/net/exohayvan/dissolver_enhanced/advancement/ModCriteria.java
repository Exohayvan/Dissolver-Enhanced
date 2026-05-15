package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
        DeferredRegister.create(Registries.TRIGGER_TYPE, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, LearnedItemCriterion> LEARNED_ITEM =
        TRIGGER_TYPES.register("learned_item", LearnedItemCriterion::new);
    public static final DeferredHolder<CriterionTrigger<?>, EmcBalanceCriterion> EMC_BALANCE =
        TRIGGER_TYPES.register("emc_balance", EmcBalanceCriterion::new);
    public static final DeferredHolder<CriterionTrigger<?>, LearnedCountCriterion> LEARNED_COUNT =
        TRIGGER_TYPES.register("learned_count", LearnedCountCriterion::new);
    public static final DeferredHolder<CriterionTrigger<?>, EmcOrbCriterion> EMC_ORB =
        TRIGGER_TYPES.register("emc_orb", EmcOrbCriterion::new);

    public static void init(IEventBus modEventBus) {
        TRIGGER_TYPES.register(modEventBus);
    }

    public static void triggerLearnedItem(Player player, String itemId) {
        if (player instanceof ServerPlayer serverPlayer) {
            LEARNED_ITEM.get().trigger(serverPlayer, itemId);
            Map<String, Object> properties = achievementProperties("learned_item");
            properties.put("item_id", itemId);
            ModAnalytics.captureAchievementEarned("learned_item", properties);
        }
    }

    public static void triggerEmcBalance(Player player, BigInteger emc) {
        if (player instanceof ServerPlayer serverPlayer) {
            EMC_BALANCE.get().trigger(serverPlayer, emc);
            Map<String, Object> properties = achievementProperties("emc_balance");
            properties.put("emc_value", emc.toString());
            ModAnalytics.captureAchievementEarned("emc_balance", properties);
        }
    }

    public static void triggerLearnedCount(Player player, int learnedItems) {
        if (player instanceof ServerPlayer serverPlayer) {
            LEARNED_COUNT.get().trigger(serverPlayer, learnedItems);
            Map<String, Object> properties = achievementProperties("learned_count");
            properties.put("learned_items", learnedItems);
            ModAnalytics.captureAchievementEarned("learned_count", properties);
        }
    }

    public static void triggerEmcOrb(Player player, BigInteger emc, String action) {
        if (player instanceof ServerPlayer serverPlayer) {
            EMC_ORB.get().trigger(serverPlayer, emc, action);
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
