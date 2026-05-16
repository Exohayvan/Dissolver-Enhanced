package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class RegistryKeyCompat {
    private RegistryKeyCompat() {
    }

    public static Item.Properties itemProperties(String id) {
        return withRegistryKey(
            new Item.Properties(),
            ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id))
        );
    }

    public static BlockBehaviour.Properties blockProperties(String id, BlockBehaviour.Properties properties) {
        return withRegistryKey(
            properties,
            ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id))
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityType<T> buildEntityType(String id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id)
        );

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

        throw new IllegalStateException("Could not find compatible EntityType builder method for " + key.location() + ".");
    }

    private static <T> T withRegistryKey(T settings, ResourceKey<?> key) {
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
            throw new IllegalStateException("Could not assign registry key " + key.location() + " to settings.", exception);
        }
    }

    private static boolean assignRegistryKeyField(Object settings, ResourceKey<?> key) {
        Class<?> type = settings.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!isResourceKeyParameter(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    field.set(settings, key);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    throw new IllegalStateException("Could not assign registry key " + key.location() + " to settings field " + field.getName() + ".", exception);
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
        if (method.getParameterCount() != 1 || !isResourceKeyParameter(method.getParameterTypes()[0])) {
            return false;
        }

        Class<?> returnType = method.getReturnType();
        return returnType == Void.TYPE || returnType.isAssignableFrom(settings.getClass()) || settings.getClass().isAssignableFrom(returnType);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<T> tryBuildEntityType(
        EntityType.Builder<T> builder,
        Method method,
        ResourceKey<EntityType<?>> key
    ) {
        if (method.getReturnType() != EntityType.class || (method.getParameterCount() != 0 && method.getParameterCount() != 1)) {
            return null;
        }

        Object[] arguments;
        if (method.getParameterCount() == 0) {
            arguments = new Object[0];
        } else if (method.getParameterTypes()[0] == String.class) {
            arguments = new Object[] { key.location().toString() };
        } else if (isResourceKeyParameter(method.getParameterTypes()[0])) {
            arguments = new Object[] { key };
        } else {
            return null;
        }

        try {
            method.setAccessible(true);
            return (EntityType<T>)method.invoke(builder, arguments);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Could not build entity type " + key.location() + ".", exception);
        }
    }

    private static boolean isResourceKeyParameter(Class<?> parameterType) {
        return parameterType == ResourceKey.class || parameterType.getName().equals("net.minecraft.resources.ResourceKey");
    }
}
