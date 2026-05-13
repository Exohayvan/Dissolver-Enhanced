package net.exohayvan.dissolver_enhanced.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

public class LearnedCountCriterion extends AbstractCriterion<LearnedCountCriterion.Conditions> {
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    public record Conditions(
        Optional<LootContextPredicate> player,
        int minItems
    ) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.INT.fieldOf("min_items").forGetter(Conditions::minItems)
        ).apply(instance, Conditions::new));

        public boolean matches(int learnedItems) {
            return learnedItems >= minItems;
        }
    }
}
