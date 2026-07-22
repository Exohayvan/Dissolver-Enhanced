package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.util.function.BiPredicate;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.advancement.compat.CriterionValues;
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

    public void trigger(ServerPlayer player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        return readConditions(jsonObject, player);
    }

    private static Conditions readConditions(JsonObject jsonObject, ContextAwarePredicate player) {
        String action = jsonObject.has("action") ? jsonObject.get("action").getAsString() : null;
        return new Conditions(player, CriterionValues.minimumEmc(jsonObject), action);
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final BiPredicate<BigInteger, String> matcher;

        public Conditions(ContextAwarePredicate player, String minEmc, String action) {
            super(ID, player);
            this.matcher = (emc, currentAction) -> CriterionValues.meetsMinimum(emc, minEmc) &&
                (action == null || action.equals(currentAction));
        }

        public boolean matches(BigInteger emc, String currentAction) {
            return matcher.test(emc, currentAction);
        }
    }
}
