package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class WorldCompat {
    private WorldCompat() {
    }

    public static World getWorld(Entity entity) {
        if (entity == null) return null;

        World world = getWorldFromMethod(entity);
        if (world != null) return world;

        return getWorldFromField(entity);
    }

    public static boolean isClient(Entity entity) {
        World world = getWorld(entity);
        return world != null && world.isClient();
    }

    private static World getWorldFromMethod(Entity entity) {
        for (String methodName : new String[] { "getWorld", "method_37908", "method_73183" }) {
            Method method = findNoArgMethod(entity.getClass(), methodName);
            if (method == null || !World.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                method.setAccessible(true);
                return (World) method.invoke(entity);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static World getWorldFromField(Entity entity) {
        Class<?> owner = entity.getClass();
        while (owner != null) {
            for (Field field : owner.getDeclaredFields()) {
                if (!World.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    return (World) field.get(entity);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }

            owner = owner.getSuperclass();
        }

        return null;
    }

    private static Method findNoArgMethod(Class<?> owner, String methodName) {
        Class<?> current = owner;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }
}
