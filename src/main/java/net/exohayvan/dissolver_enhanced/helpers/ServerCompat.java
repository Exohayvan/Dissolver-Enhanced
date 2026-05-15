package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerCompat {
    private ServerCompat() {
    }

    public static MinecraftServer getServer(Entity entity) {
        if (entity == null) return null;

        MinecraftServer server = getServerFromMethod(entity);
        if (server != null) return server;

        if (entity instanceof ServerPlayerEntity) {
            server = getServerFromField(entity);
            if (server != null) return server;
        }

        return null;
    }

    private static MinecraftServer getServerFromMethod(Entity entity) {
        for (String methodName : new String[] { "getServer", "method_5682" }) {
            Method method = findNoArgMethod(entity.getClass(), methodName);
            if (method == null || !MinecraftServer.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                method.setAccessible(true);
                return (MinecraftServer) method.invoke(entity);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static MinecraftServer getServerFromField(Entity entity) {
        Class<?> owner = entity.getClass();
        while (owner != null) {
            for (Field field : owner.getDeclaredFields()) {
                if (!MinecraftServer.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    return (MinecraftServer) field.get(entity);
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
