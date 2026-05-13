package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;

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

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        int minItems = jsonObject.has("min_items") ? jsonObject.get("min_items").getAsInt() : 0;
        return new Conditions(player, minItems);
    }

    public void trigger(ServerPlayer player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final int minItems;

        public Conditions(ContextAwarePredicate player, int minItems) {
            super(ID, player);
            this.minItems = minItems;
        }

        public boolean matches(int learnedItems) {
            return learnedItems >= minItems;
        }
    }
}
