package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ModCriteria {
    public static final LearnedItemCriterion LEARNED_ITEM = CriteriaTriggers.register(
        DissolverEnhanced.MOD_ID + ":learned_item",
        new LearnedItemCriterion()
    );
    public static final EmcBalanceCriterion EMC_BALANCE = CriteriaTriggers.register(
        DissolverEnhanced.MOD_ID + ":emc_balance",
        new EmcBalanceCriterion()
    );

    public static void init() {
    }

    public static void triggerLearnedItem(Player player, String itemId) {
        if (player instanceof ServerPlayer serverPlayer) {
            LEARNED_ITEM.trigger(serverPlayer, itemId);
        }
    }

    public static void triggerEmcBalance(Player player, BigInteger emc) {
        if (player instanceof ServerPlayer serverPlayer) {
            EMC_BALANCE.trigger(serverPlayer, emc);
        }
    }
}
