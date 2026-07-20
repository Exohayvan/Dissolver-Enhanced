package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class LearnedCountCriterion1203 extends SimpleCriterionTrigger<LearnedCountCriterion1203.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, int learnedItems) {
        trigger(player, conditions -> conditions.matches(learnedItems));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, int minItems) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.INT.optionalFieldOf("min_items", 0).forGetter(Conditions::minItems)
        ).apply(instance, Conditions::new));

        public boolean matches(int learnedItems) {
            return learnedItems >= minItems;
        }
    }
}
