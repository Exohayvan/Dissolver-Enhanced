package net.exohayvan.dissolver_enhanced.advancement;

import com.google.gson.JsonObject;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class LearnedItemCriterion extends SimpleCriterionTrigger<LearnedItemCriterion.Conditions> {
    public static final ResourceLocation ID = new ResourceLocation("dissolver_enhanced", "learned_item");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject jsonObject, ContextAwarePredicate player, DeserializationContext context) {
        String item = jsonObject.has("item") ? jsonObject.get("item").getAsString() : null;
        boolean hasExternalNamespace = jsonObject.has("external_namespace");
        Boolean externalNamespace = hasExternalNamespace ? jsonObject.get("external_namespace").getAsBoolean() : null;
        return new Conditions(player, item, externalNamespace);
    }

    public void trigger(ServerPlayer player, String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        trigger(player, conditions -> conditions.matches(baseItemId));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final String item;
        private final Boolean externalNamespace;

        public Conditions(ContextAwarePredicate player, String item, Boolean externalNamespace) {
            super(ID, player);
            this.item = item;
            this.externalNamespace = externalNamespace;
        }

        public boolean matches(String itemId) {
            if (item != null && !item.equals(itemId)) {
                return false;
            }

            if (externalNamespace != null && externalNamespace != isExternalNamespace(itemId)) {
                return false;
            }

            return true;
        }

        private static boolean isExternalNamespace(String itemId) {
            int namespaceEnd = itemId.indexOf(":");
            return namespaceEnd > 0 && !"minecraft".equals(itemId.substring(0, namespaceEnd));
        }
    }
}
