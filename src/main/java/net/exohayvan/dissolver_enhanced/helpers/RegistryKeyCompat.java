package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class RegistryKeyCompat {
    private RegistryKeyCompat() {
    }

    public static Item.Properties itemProperties(String id) {
        return withRegistryKey(
            new Item.Properties(),
            ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id)),
            Item.class
        );
    }

    public static BlockBehaviour.Properties blockProperties(String id, BlockBehaviour.Properties properties) {
        return withRegistryKey(
            properties,
            ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, id)),
            Block.class
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

    private static <T> T withRegistryKey(T settings, ResourceKey<?> key, Class<?> registryValueType) {
        Method method = registryKeyMethod(settings, registryValueType);
        if (method != null) {
            try {
                method.setAccessible(true);
                method.invoke(settings, key);
                return settings;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                throw new IllegalStateException("Could not assign registry key " + key.location() + " to settings.", exception);
            }
        }

        assignRegistryKeyField(settings, key, registryValueType);
        return settings;
    }

    private static boolean assignRegistryKeyField(Object settings, ResourceKey<?> key, Class<?> registryValueType) {
        Class<?> type = settings.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!isResourceKeyParameter(field.getType())) {
                    continue;
                }
                if (!isRegistryKeyFieldName(field.getName()) && !isResourceKeyTarget(field.getGenericType(), registryValueType)) {
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

    private static Method registryKeyMethod(Object settings, Class<?> registryValueType) {
        for (Method method : settings.getClass().getMethods()) {
            if (isRegistryKeySettingsMethod(settings, method, registryValueType)) {
                return method;
            }
        }

        for (Method method : settings.getClass().getDeclaredMethods()) {
            if (isRegistryKeySettingsMethod(settings, method, registryValueType)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isRegistryKeySettingsMethod(Object settings, Method method, Class<?> registryValueType) {
        if (method.getParameterCount() != 1 || !isResourceKeyParameter(method.getParameterTypes()[0])) {
            return false;
        }

        if (!isRegistryKeyMethodName(method.getName()) && !isResourceKeyTarget(method.getGenericParameterTypes()[0], registryValueType)) {
            return false;
        }

        Class<?> returnType = method.getReturnType();
        return returnType == Void.TYPE || returnType.isAssignableFrom(settings.getClass()) || settings.getClass().isAssignableFrom(returnType);
    }

    private static boolean isRegistryKeyMethodName(String name) {
        String lowerName = name.toLowerCase(java.util.Locale.ROOT);
        return lowerName.equals("setid")
            || lowerName.equals("id")
            || lowerName.equals("registrykey")
            || lowerName.equals("setregistrykey")
            || lowerName.contains("registrykey");
    }

    private static boolean isRegistryKeyFieldName(String name) {
        String lowerName = name.toLowerCase(java.util.Locale.ROOT);
        return lowerName.equals("id")
            || lowerName.equals("key")
            || lowerName.equals("registrykey")
            || lowerName.contains("registrykey");
    }

    private static boolean isResourceKeyTarget(Type type, Class<?> registryValueType) {
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return false;
        }

        if (!isResourceKeyType(parameterizedType.getRawType())) {
            return false;
        }

        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        return typeArguments.length == 1 && isRegistryValueType(typeArguments[0], registryValueType);
    }

    private static boolean isResourceKeyType(Type type) {
        if (type == ResourceKey.class) {
            return true;
        }
        if (type instanceof Class<?> typeClass) {
            return typeClass.getName().equals("net.minecraft.resources.ResourceKey");
        }

        return false;
    }

    private static boolean isRegistryValueType(Type type, Class<?> registryValueType) {
        if (type == registryValueType) {
            return true;
        }
        if (type instanceof Class<?> typeClass) {
            return typeClass.getName().equals(registryValueType.getName())
                || typeClass.getSimpleName().equals(registryValueType.getSimpleName());
        }

        return false;
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
