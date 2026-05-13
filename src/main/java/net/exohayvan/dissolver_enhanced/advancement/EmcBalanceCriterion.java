package net.exohayvan.dissolver_enhanced.advancement;

import java.math.BigInteger;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EmcBalanceCriterion extends AbstractCriterion<EmcBalanceCriterion.Conditions> {
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, BigInteger emc) {
        trigger(player, conditions -> conditions.matches(emc));
    }

    public record Conditions(
        Optional<LootContextPredicate> player,
        String minEmc
    ) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.fieldOf("min_emc").forGetter(Conditions::minEmc)
        ).apply(instance, Conditions::new));

        public boolean matches(BigInteger emc) {
            return EmcNumber.nonNegative(emc).compareTo(EmcNumber.parse(minEmc)) >= 0;
        }
    }
}
