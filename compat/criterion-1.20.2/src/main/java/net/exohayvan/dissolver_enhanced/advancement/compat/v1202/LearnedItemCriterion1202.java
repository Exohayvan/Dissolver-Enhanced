package net.exohayvan.dissolver_enhanced.advancement.compat.v1202;

import com.google.gson.JsonObject;
import net.exohayvan.dissolver_enhanced.advancement.compat.CriterionValues;
import java.util.Optional;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class LearnedItemCriterion1202 extends SimpleCriterionTrigger<LearnedItemCriterion1202.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "learned_item");

    @Override
    protected Conditions createInstance(JsonObject jsonObject, Optional<ContextAwarePredicate> player, DeserializationContext context) {
        return new Conditions(player, CriterionValues.optionalItem(jsonObject), CriterionValues.optionalExternalNamespace(jsonObject));
    }

    public void trigger(ServerPlayer player, String itemId) {
        String baseItemId = CriterionValues.baseItemId(itemId);
        trigger(player, conditions -> conditions.matches(baseItemId));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String item;
        private final Boolean externalNamespace;

        public Conditions(Optional<ContextAwarePredicate> player, String item, Boolean externalNamespace) {
            super(player);
            this.item = item;
            this.externalNamespace = externalNamespace;
        }

        public boolean matches(String itemId) {
            return CriterionValues.matchesLearnedItem(item, externalNamespace, itemId);
        }
    }
}
