package net.exohayvan.dissolver_enhanced.particle;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModParticles {
    public static final SimpleParticleType CRYSTAL = new SimpleParticleType(false);
    // public static final SimpleParticleType CRYSTAL = ParticleTypes.END_ROD.getType();

    // HELPERS

    private static SimpleParticleType registerParticle(String id, SimpleParticleType particle) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation(DissolverEnhanced.MOD_ID, id), particle);
    }

    // INITIALIZE
    
    public static void init() {
        registerParticle("crystal_particle", CRYSTAL);
        // ParticleTypes.END_ROD
        // EndRodParticle
        // EndRodBlock
    }
}
