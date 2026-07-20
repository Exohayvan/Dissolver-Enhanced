package net.exohayvan.dissolver_enhanced.advancement.compat.v1202;

import com.google.gson.JsonObject;
import java.util.Optional;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class LearnedCountCriterion1202 extends SimpleCriterionTrigger<LearnedCountCriterion1202.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "learned_count");

    @Override
    protected Conditions createInstance(JsonObject jsonObject, Optional<ContextAwarePredicate> player, DeserializationContext context) {
        int minItems = jsonObject.has("min_items") ? jsonObject.get("min_items").getAsInt() : 0;
        return new Conditions(player, minItems);
    }

    public void trigger(ServerPlayer player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final int minItems;

        public Conditions(Optional<ContextAwarePredicate> player, int minItems) {
            super(player);
            this.minItems = minItems;
        }

        public boolean matches(int learnedItems) {
            return learnedItems >= minItems;
        }
    }
}
