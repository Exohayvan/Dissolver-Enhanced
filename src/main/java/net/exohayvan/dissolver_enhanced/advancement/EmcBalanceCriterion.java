package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EmcBalanceCriterion extends SimpleCriterionTrigger<EmcBalanceCriterion.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String minEmc) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.STRING.optionalFieldOf("min_emc", "0").forGetter(Conditions::minEmc)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc) {
            return EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) >= 0;
        }
    }
}
