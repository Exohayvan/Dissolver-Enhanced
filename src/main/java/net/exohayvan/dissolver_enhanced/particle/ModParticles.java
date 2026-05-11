package net.exohayvan.dissolver_enhanced.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public class ModParticles {
    public static final SimpleParticleType CRYSTAL = FabricParticleTypes.simple();
    // public static final SimpleParticleType CRYSTAL = ParticleTypes.END_ROD.getType();

    // HELPERS

    private static SimpleParticleType registerParticle(String id, SimpleParticleType particle) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id), particle);
    }

    // INITIALIZE
    
    public static void init() {
        registerParticle("crystal_particle", CRYSTAL);
        // ParticleTypes.END_ROD
        // EndRodParticle
        // EndRodBlock
    }
}
