package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.util.function.Predicate;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.advancement.compat.CriterionValues;

public class EmcBalanceCriterion extends SimpleCriterionTrigger<EmcBalanceCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "emc_balance");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        return new Conditions(player, CriterionValues.minimumEmc(jsonObject));
    }

    public void trigger(ServerPlayer player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final Predicate<BigInteger> matchesMinimum;

        public Conditions(ContextAwarePredicate player, String minEmc) {
            super(ID, player);
            this.matchesMinimum = emc -> CriterionValues.meetsMinimum(emc, minEmc);
        }

        public boolean matches(BigInteger emc) {
            return matchesMinimum.test(emc);
        }
    }
}
