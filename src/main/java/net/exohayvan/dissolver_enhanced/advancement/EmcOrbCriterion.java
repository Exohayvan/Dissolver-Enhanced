package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

public class EmcOrbCriterion extends AbstractCriterion<EmcOrbCriterion.Conditions> {
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, BigInteger emc, String action) {
        trigger(player, conditions -> conditions.matches(emc, action));
    }

    public record Conditions(
        Optional<LootContextPredicate> player,
        String minEmc,
        Optional<String> action
    ) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.fieldOf("min_emc").forGetter(Conditions::minEmc),
            Codec.STRING.optionalFieldOf("action").forGetter(Conditions::action)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc, String currentAction) {
            if (EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) < 0) return false;
            return action.isEmpty() || action.get().equals(currentAction);
        }
    }
}
