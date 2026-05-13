package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

import java.math.BigInteger;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EmcBalanceCriterion extends SimpleCriterionTrigger<EmcBalanceCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "emc_balance");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        String minEmc = jsonObject.has("min_emc") ? jsonObject.get("min_emc").getAsString() : "0";
        return new Conditions(player, minEmc);
    }

    public void trigger(ServerPlayer player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String minEmc;

        public Conditions(ContextAwarePredicate player, String minEmc) {
            super(ID, player);
            this.minEmc = minEmc;
        }

        public boolean matches(BigInteger emc) {
            return EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) >= 0;
        }
    }
}
