package net.exohayvan.dissolver_enhanced.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerCompat {
    private ServerCompat() {
    }

    public static MinecraftServer getServer(Entity entity) {
        if (entity == null) return null;

        MinecraftServer server = ReflectionCompat.invokeNoArg(
            entity,
            MinecraftServer.class,
            "getServer",
            "method_5682"
        );
        if (server != null || !(entity instanceof ServerPlayerEntity)) {
            return server;
        }

        return ReflectionCompat.readField(entity, MinecraftServer.class);
    }
}
