package net.exohayvan.dissolver_enhanced.advancement.compat.v1202;

import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.util.Optional;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class EmcBalanceCriterion1202 extends SimpleCriterionTrigger<EmcBalanceCriterion1202.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "emc_balance");

    @Override
    protected Conditions createInstance(JsonObject jsonObject, Optional<ContextAwarePredicate> player, DeserializationContext context) {
        String minEmc = jsonObject.has("min_emc") ? jsonObject.get("min_emc").getAsString() : "0";
        return new Conditions(player, minEmc);
    }

    public void trigger(ServerPlayer player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String minEmc;

        public Conditions(Optional<ContextAwarePredicate> player, String minEmc) {
            super(player);
            this.minEmc = minEmc;
        }

        public boolean matches(BigInteger emc) {
            return CriterionValueCompat.nonNegative(emc).compareTo(CriterionValueCompat.parse(minEmc)) >= 0;
        }
    }
}
