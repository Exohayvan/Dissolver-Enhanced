package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class RegistryKeyCompat {
    private RegistryKeyCompat() {
    }

    @SuppressWarnings("unchecked")
    public static ResourceKey<Registry<CreativeModeTab>> creativeModeTab() {
        ResourceLocation location = resourceLocation("minecraft", "creative_mode_tab");
        for (Method method : ResourceKey.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1 &&
                method.getParameterTypes()[0] == ResourceLocation.class && method.getReturnType() == ResourceKey.class) {
                return (ResourceKey<Registry<CreativeModeTab>>) invoke(method, null, location);
            }
        }
        throw new IllegalStateException("Could not create the creative-mode-tab registry key.");
    }

    private static ResourceLocation resourceLocation(String namespace, String path) {
        try {
            Constructor<ResourceLocation> constructor = ResourceLocation.class.getDeclaredConstructor(String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(namespace, path);
        } catch (ReflectiveOperationException ignored) {
            for (Method method : ResourceLocation.class.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == ResourceLocation.class &&
                    method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class &&
                    method.getParameterTypes()[1] == String.class) {
                    return (ResourceLocation) invoke(method, null, namespace, path);
                }
            }
            throw new IllegalStateException("Could not construct a ResourceLocation.");
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not invoke registry compatibility method.", exception);
        }
    }
}
