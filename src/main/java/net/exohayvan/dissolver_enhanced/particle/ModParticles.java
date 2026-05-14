package net.exohayvan.dissolver_enhanced.particle;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<SimpleParticleType> CRYSTAL = PARTICLES.register(
            "crystal_particle",
            () -> new SimpleParticleType(false)
    );

    public static void init(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
