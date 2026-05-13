package net.exohayvan.dissolver_enhanced.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class LearnedCountCriterion extends SimpleCriterionTrigger<LearnedCountCriterion.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        int minItems
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.INT.fieldOf("min_items").forGetter(Conditions::minItems)
        ).apply(instance, Conditions::new));

        public boolean matches(int learnedItems) {
            return learnedItems >= minItems;
        }
    }
}
