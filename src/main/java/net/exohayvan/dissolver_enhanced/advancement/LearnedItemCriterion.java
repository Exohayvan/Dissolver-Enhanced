package net.exohayvan.dissolver_enhanced.advancement;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class LearnedItemCriterion extends SimpleCriterionTrigger<LearnedItemCriterion.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        trigger(player, conditions -> conditions.matches(baseItemId));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        Optional<String> item,
        Optional<List<String>> items,
        Optional<Boolean> externalNamespace
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.optionalFieldOf("item").forGetter(Conditions::item),
            Codec.STRING.listOf().optionalFieldOf("items").forGetter(Conditions::items),
            Codec.BOOL.optionalFieldOf("external_namespace").forGetter(Conditions::externalNamespace)
        ).apply(instance, Conditions::new));

        public boolean matches(String itemId) {
            if (item.isPresent() && !item.get().equals(itemId)) {
                return false;
            }

            if (items.isPresent() && !items.get().contains(itemId)) {
                return false;
            }

            if (externalNamespace.isPresent() && externalNamespace.get() != isExternalNamespace(itemId)) {
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
