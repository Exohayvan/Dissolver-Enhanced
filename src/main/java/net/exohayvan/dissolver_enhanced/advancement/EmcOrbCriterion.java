package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class EmcOrbCriterion extends SimpleCriterionTrigger<EmcOrbCriterion.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String minEmc, Optional<String> action) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.STRING.optionalFieldOf("min_emc", "0").forGetter(Conditions::minEmc),
                Codec.STRING.optionalFieldOf("action").forGetter(Conditions::action)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc, String currentAction) {
            if (EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) < 0) return false;
            return action.isEmpty() || action.get().equals(currentAction);
        }
    }
}
