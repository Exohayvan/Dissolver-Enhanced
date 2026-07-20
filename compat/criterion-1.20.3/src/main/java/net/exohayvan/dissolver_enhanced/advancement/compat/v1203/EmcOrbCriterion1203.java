package net.exohayvan.dissolver_enhanced.advancement.compat.v1203;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class EmcOrbCriterion1203 extends SimpleCriterionTrigger<EmcOrbCriterion1203.Conditions> {
    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    public record Conditions(
        Optional<ContextAwarePredicate> player,
        String minEmc,
        Optional<String> action
    ) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.optionalFieldOf("min_emc", "0").forGetter(Conditions::minEmc),
            Codec.STRING.optionalFieldOf("action").forGetter(Conditions::action)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc, String currentAction) {
            if (CriterionValueCompat.nonNegative(emc).compareTo(CriterionValueCompat.parse(minEmc)) < 0) return false;
            return action.isEmpty() || action.get().equals(currentAction);
        }
    }
}
