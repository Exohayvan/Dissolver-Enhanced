package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

import java.util.function.IntPredicate;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class LearnedCountCriterion extends SimpleCriterionTrigger<LearnedCountCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "learned_count");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        return readConditions(jsonObject, player);
    }

    private static Conditions readConditions(JsonObject jsonObject, ContextAwarePredicate player) {
        int minItems = jsonObject.has("min_items") ? jsonObject.get("min_items").getAsInt() : 0;
        return new Conditions(player, minItems);
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final IntPredicate matcher;

        public Conditions(ContextAwarePredicate player, int minItems) {
            super(ID, player);
            this.matcher = learnedItems -> learnedItems >= minItems;
        }

        public boolean matches(int learnedItems) {
            return matcher.test(learnedItems);
        }
    }
}
