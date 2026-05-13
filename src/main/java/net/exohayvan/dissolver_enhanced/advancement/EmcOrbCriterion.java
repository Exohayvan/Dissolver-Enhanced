package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class EmcOrbCriterion extends SimpleCriterionTrigger<EmcOrbCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "emc_orb");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        String minEmc = jsonObject.has("min_emc") ? jsonObject.get("min_emc").getAsString() : "0";
        String action = jsonObject.has("action") ? jsonObject.get("action").getAsString() : null;
        return new Conditions(player, minEmc, action);
    }

    public void trigger(ServerPlayer player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String minEmc;
        private final String action;

        public Conditions(ContextAwarePredicate player, String minEmc, String action) {
            super(ID, player);
            this.minEmc = minEmc;
            this.action = action;
        }

        public boolean matches(BigInteger emc, String currentAction) {
            if (EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) < 0) return false;
            return action == null || action.equals(currentAction);
        }
    }
}
