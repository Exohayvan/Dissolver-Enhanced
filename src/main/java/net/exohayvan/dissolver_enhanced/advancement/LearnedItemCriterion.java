package net.exohayvan.dissolver_enhanced.advancement;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

public class LearnedItemCriterion extends AbstractCriterion<LearnedItemCriterion.Conditions> {
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, String itemId) {
        String baseItemId = EMCKey.baseItemId(itemId);
        trigger(player, conditions -> conditions.matches(baseItemId));
    }

    public record Conditions(
        Optional<LootContextPredicate> player,
        Optional<String> item,
        Optional<List<String>> items,
        Optional<Boolean> externalNamespace
    ) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
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
