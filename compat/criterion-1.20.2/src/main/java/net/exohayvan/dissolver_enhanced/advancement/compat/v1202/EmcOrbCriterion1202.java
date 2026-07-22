package net.exohayvan.dissolver_enhanced.advancement.compat.v1202;

import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.util.Optional;

import net.exohayvan.dissolver_enhanced.advancement.compat.CriterionValues;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class EmcOrbCriterion1202 extends SimpleCriterionTrigger<EmcOrbCriterion1202.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "emc_orb");

    @Override
    protected Conditions createInstance(JsonObject jsonObject, Optional<ContextAwarePredicate> player, DeserializationContext context) {
        String action = jsonObject.has("action") ? jsonObject.get("action").getAsString() : null;
        return new Conditions(player, CriterionValues.minimumEmc(jsonObject), action);
    }

    public void trigger(ServerPlayer player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String minEmc;
        private final String action;

        public Conditions(Optional<ContextAwarePredicate> player, String minEmc, String action) {
            super(player);
            this.minEmc = minEmc;
            this.action = action;
        }

        public boolean matches(BigInteger emc, String currentAction) {
            return CriterionValues.meetsMinimum(emc, minEmc) && (action == null || action.equals(currentAction));
        }
    }
}
