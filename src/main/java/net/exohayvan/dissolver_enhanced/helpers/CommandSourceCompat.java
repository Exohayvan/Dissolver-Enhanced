package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Method;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CommandSourceCompat {
    private CommandSourceCompat() {
    }

    public static boolean hasPermissionLevel(ServerCommandSource source, int level) {
        Boolean directPermission = invokeBooleanInt(source, level, "hasPermissionLevel", "method_9259");
        if (directPermission != null) {
            return directPermission;
        }

        if (level <= 0) {
            return true;
        }

        ServerPlayerEntity player = getPlayer(source);
        return player != null && isOperator(source, player);
    }

    private static Boolean invokeBooleanInt(Object target, int value, String... methodNames) {
        if (target == null) return null;

        for (Method method : target.getClass().getMethods()) {
            if (!hasName(method, methodNames) ||
                    method.getParameterCount() != 1 ||
                    method.getParameterTypes()[0] != int.class ||
                    method.getReturnType() != boolean.class) {
                continue;
            }

            try {
                method.setAccessible(true);
                return (Boolean) method.invoke(target, value);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static ServerPlayerEntity getPlayer(ServerCommandSource source) {
        Object player = invokeNoArg(source, "getPlayer", "getPlayerOrThrow", "method_9207", "method_44023");
        return player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
    }

    private static boolean isOperator(ServerCommandSource source, ServerPlayerEntity player) {
        MinecraftServer server = getServer(source);
        Object playerManager = invokeNoArg(server, "getPlayerManager", "method_3760");
        Object profile = invokeNoArg(player, "getGameProfile", "method_72498");

        if (playerManager == null || profile == null) {
            return false;
        }

        for (Method method : playerManager.getClass().getMethods()) {
            if ((!method.getName().equals("isOperator") && !method.getName().equals("method_14569")) ||
                    method.getParameterCount() != 1 ||
                    method.getReturnType() != boolean.class ||
                    !method.getParameterTypes()[0].isAssignableFrom(profile.getClass())) {
                continue;
            }

            try {
                method.setAccessible(true);
                return (Boolean) method.invoke(playerManager, profile);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return false;
    }

    private static MinecraftServer getServer(ServerCommandSource source) {
        Object server = invokeNoArg(source, "getServer", "method_9211");
        return server instanceof MinecraftServer ? (MinecraftServer) server : null;
    }

    private static Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) return null;

        for (Method method : target.getClass().getMethods()) {
            if (!hasName(method, methodNames) || method.getParameterCount() != 0) {
                continue;
            }

            try {
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static boolean hasName(Method method, String... methodNames) {
        for (String methodName : methodNames) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
