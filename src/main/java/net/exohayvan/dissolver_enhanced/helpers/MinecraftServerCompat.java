package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.server.MinecraftServer;

public final class MinecraftServerCompat {
    private MinecraftServerCompat() {
    }

    public static int connectedPlayerCount(MinecraftServer server) {
        Object playerList = invokeNoArgReturning(server, "net.minecraft.server.players.PlayerList");
        for (Method method : playerList.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && List.class.isAssignableFrom(method.getReturnType())) {
                Object value = invoke(method, playerList);
                if (value instanceof List<?> players) {
                    return players.size();
                }
            }
        }
        throw new IllegalStateException("Could not find the connected-player collection.");
    }

    private static Object invokeNoArgReturning(Object target, String returnTypeName) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().getName().equals(returnTypeName)) {
                return invoke(method, target);
            }
        }
        throw new IllegalStateException("Could not find method returning " + returnTypeName);
    }

    private static Object invoke(Method method, Object target) {
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not inspect connected players.", exception);
        }
    }
}
