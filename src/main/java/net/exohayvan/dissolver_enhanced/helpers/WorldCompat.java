package net.exohayvan.dissolver_enhanced.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class WorldCompat {
    private WorldCompat() {
    }

    public static World getWorld(Entity entity) {
        if (entity == null) return null;

        World world = ReflectionCompat.invokeNoArg(
            entity,
            World.class,
            "getWorld",
            "method_37908",
            "method_73183"
        );
        return world != null ? world : ReflectionCompat.readField(entity, World.class);
    }

    public static boolean isClient(Entity entity) {
        World world = getWorld(entity);
        return world != null && world.isClient();
    }
}
