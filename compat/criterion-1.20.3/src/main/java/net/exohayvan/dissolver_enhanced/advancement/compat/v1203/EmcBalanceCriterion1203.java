package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class EmcBalanceCriterion1203 extends SimpleCriterionTrigger<EmcBalanceCriterion1203.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String minEmc) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.optionalFieldOf("min_emc", "0").forGetter(Conditions::minEmc)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc) {
            return CriterionValueCompat.nonNegative(emc).compareTo(CriterionValueCompat.parse(minEmc)) >= 0;
        }
    }
}
