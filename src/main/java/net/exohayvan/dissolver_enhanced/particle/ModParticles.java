package net.exohayvan.dissolver_enhanced.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModParticles {
    public static final SimpleParticleType CRYSTAL = FabricParticleTypes.simple();
    // public static final SimpleParticleType CRYSTAL = ParticleTypes.END_ROD.getType();

    // HELPERS

    private static SimpleParticleType registerParticle(String id, SimpleParticleType particle) {
        return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(DissolverEnhanced.MOD_ID, id), particle);
    }

    // INITIALIZE
    
    public static void init() {
        registerParticle("crystal_particle", CRYSTAL);
        // ParticleTypes.END_ROD
        // EndRodParticle
        // EndRodBlock
    }
}
