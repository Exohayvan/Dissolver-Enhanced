package net.exohayvan.dissolver_enhanced.advancement;

import com.mojang.serialization.Codec;

import net.minecraft.advancement.criterion.AbstractCriterion;

abstract class CodecBackedCriterion<C extends AbstractCriterion.Conditions> extends AbstractCriterion<C> {
    private final Codec<C> conditionsCodec;

    protected CodecBackedCriterion(Codec<C> conditionsCodec) {
        this.conditionsCodec = conditionsCodec;
    }

    @Override
    public final Codec<C> getConditionsCodec() {
        return conditionsCodec;
    }
}
