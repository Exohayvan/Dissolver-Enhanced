package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class RegistryKeyCompat {
    private RegistryKeyCompat() {
    }

    public static Item.Settings itemSettings(String id) {
        return withRegistryKey(
            new Item.Settings(),
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(DissolverEnhanced.MOD_ID, id))
        );
    }

    public static AbstractBlock.Settings blockSettings(String id, AbstractBlock.Settings settings) {
        return withRegistryKey(
            settings,
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(DissolverEnhanced.MOD_ID, id))
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityType<T> buildEntityType(String id, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(DissolverEnhanced.MOD_ID, id));
        for (Method method : builder.getClass().getMethods()) {
            EntityType<T> entityType = tryBuildEntityType(builder, method, key);
            if (entityType != null) {
                return entityType;
            }
        }

        for (Method method : builder.getClass().getDeclaredMethods()) {
            EntityType<T> entityType = tryBuildEntityType(builder, method, key);
            if (entityType != null) {
                return entityType;
            }
        }

        throw new IllegalStateException("Could not find compatible EntityType builder method for " + key.getValue() + ".");
    }

    private static <T> T withRegistryKey(T settings, RegistryKey<?> key) {
        if (assignRegistryKeyField(settings, key)) {
            return settings;
        }

        Method method = registryKeyMethod(settings);
        if (method == null) {
            return settings;
        }

        try {
            method.setAccessible(true);
            method.invoke(settings, key);
            return settings;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Could not assign registry key " + key.getValue() + " to settings.", exception);
        }
    }

    private static boolean assignRegistryKeyField(Object settings, RegistryKey<?> key) {
        Class<?> type = settings.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!isRegistryKeyParameter(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    field.set(settings, key);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    throw new IllegalStateException("Could not assign registry key " + key.getValue() + " to settings field " + field.getName() + ".", exception);
                }
            }

            type = type.getSuperclass();
        }

        return false;
    }

    private static Method registryKeyMethod(Object settings) {
        for (Method method : settings.getClass().getMethods()) {
            if (isRegistryKeySettingsMethod(settings, method)) {
                return method;
            }
        }

        for (Method method : settings.getClass().getDeclaredMethods()) {
            if (isRegistryKeySettingsMethod(settings, method)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isRegistryKeySettingsMethod(Object settings, Method method) {
        if (method.getParameterCount() != 1) {
            return false;
        }

        Class<?> parameterType = method.getParameterTypes()[0];
        if (parameterType != RegistryKey.class && !parameterType.getName().equals("net.minecraft.class_5321")) {
            return false;
        }

        Class<?> returnType = method.getReturnType();
        return returnType == Void.TYPE || returnType.isAssignableFrom(settings.getClass()) || settings.getClass().isAssignableFrom(returnType);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<T> tryBuildEntityType(EntityType.Builder<T> builder, Method method, RegistryKey<EntityType<?>> key) {
        if (method.getReturnType() != EntityType.class || (method.getParameterCount() != 0 && method.getParameterCount() != 1)) {
            return null;
        }

        Object[] arguments;
        if (method.getParameterCount() == 0) {
            arguments = new Object[0];
        } else if (isRegistryKeyParameter(method.getParameterTypes()[0])) {
            arguments = new Object[] { key };
        } else {
            return null;
        }

        try {
            method.setAccessible(true);
            return (EntityType<T>)method.invoke(builder, arguments);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Could not build entity type " + key.getValue() + ".", exception);
        }
    }

    private static boolean isRegistryKeyParameter(Class<?> parameterType) {
        return parameterType == RegistryKey.class || parameterType.getName().equals("net.minecraft.class_5321");
    }
}
