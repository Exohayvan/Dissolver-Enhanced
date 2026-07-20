package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class LearnedItemCriterion1203 extends SimpleCriterionTrigger<LearnedItemCriterion1203.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, String itemId) {
        String baseItemId = CriterionValueCompat.baseItemId(itemId);
        trigger(player, conditions -> conditions.matches(baseItemId));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        Optional<String> item,
        Optional<Boolean> externalNamespace
    ) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.optionalFieldOf("item").forGetter(Conditions::item),
            Codec.BOOL.optionalFieldOf("external_namespace").forGetter(Conditions::externalNamespace)
        ).apply(instance, Conditions::new));

        public boolean matches(String itemId) {
            if (item.isPresent() && !item.get().equals(itemId)) {
                return false;
            }

            return externalNamespace.isEmpty() || externalNamespace.get() == isExternalNamespace(itemId);
        }

        private static boolean isExternalNamespace(String itemId) {
            int namespaceEnd = itemId.indexOf(":");
            return namespaceEnd > 0 && !"minecraft".equals(itemId.substring(0, namespaceEnd));
        }
    }
}
