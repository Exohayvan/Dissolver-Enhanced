package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
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
        }
    }

    public static void triggerEmcBalance(PlayerEntity player, BigInteger emc) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EMC_BALANCE.trigger(serverPlayer, emc);
        }
    }

    public static void triggerLearnedCount(PlayerEntity player, int learnedItems) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LEARNED_COUNT.trigger(serverPlayer, learnedItems);
        }
    }

    public static void triggerEmcOrb(PlayerEntity player, BigInteger emc, String action) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            EMC_ORB.trigger(serverPlayer, emc, action);
        }
    }
}
