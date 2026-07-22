package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public final class ReflectionCompat {
    private ReflectionCompat() {
    }

    public static Method findCompatibleMethod(Class<?> type, String name, Object... arguments) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != arguments.length) continue;

            Class<?>[] parameters = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameters.length; index++) {
                if (!isCompatible(parameters[index], arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) return method;
        }
        return null;
    }

    public static ResourceLocation resourceLocation(
        String namespace,
        String path,
        Function<Method, ResourceLocation> staticFactoryInvoker
    ) {
        try {
            Constructor<ResourceLocation> constructor = ResourceLocation.class.getDeclaredConstructor(String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(namespace, path);
        } catch (ReflectiveOperationException ignored) {
            for (Method method : ResourceLocation.class.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == ResourceLocation.class &&
                    parameters.length == 2 && parameters[0] == String.class && parameters[1] == String.class) {
                    return staticFactoryInvoker.apply(method);
                }
            }
            throw new IllegalStateException("Could not construct a ResourceLocation.");
        }
    }

    private static boolean isCompatible(Class<?> parameter, Object argument) {
        if (argument == null) return !parameter.isPrimitive();
        if (!parameter.isPrimitive()) return parameter.isInstance(argument);
        return (parameter == int.class && argument instanceof Integer) ||
            (parameter == boolean.class && argument instanceof Boolean);
    }
}