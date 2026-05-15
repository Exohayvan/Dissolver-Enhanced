package net.exohayvan.dissolver_enhanced.particle;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRYSTAL = PARTICLES.register(
            "crystal_particle",
            () -> new SimpleParticleType(false)
    );

    public static void init(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
