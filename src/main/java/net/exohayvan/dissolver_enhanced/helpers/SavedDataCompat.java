package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class SavedDataCompat {
    private SavedDataCompat() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends SavedData> T get(
        DimensionDataStorage storage,
        String id,
        Supplier<T> supplier,
        Function<CompoundTag, T> loader
    ) {
        for (Method method : storage.getClass().getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 2 || parameters[1] != String.class ||
                !SavedData.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            Object firstArgument;
            if (parameters[0].isInstance(loader)) {
                firstArgument = loader;
            } else if (parameters[0].getName().equals("net.minecraft.world.level.saveddata.SavedData$Factory") &&
                isExistingLookupName(method.getName())) {
                firstArgument = createFactory(parameters[0], supplier, loader);
            } else {
                continue;
            }
            return (T) invoke(method, storage, firstArgument, id);
        }
        throw new IllegalStateException("DimensionDataStorage has no compatible saved-data lookup method.");
    }

    static boolean isExistingLookupName(String methodName) {
        return methodName.equals("get") || methodName.equals("m_164858_");
    }

    private static Object createFactory(Class<?> factoryType, Supplier<?> supplier, Function<?, ?> loader) {
        for (Constructor<?> constructor : factoryType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 3 || !parameters[0].isInstance(supplier) || !parameters[1].isInstance(loader)) {
                continue;
            }
            Object dataFixType = staticFieldAssignableTo(parameters[2], "LEVEL");
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(supplier, loader, dataFixType);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                throw new IllegalStateException("Could not construct SavedData.Factory.", exception);
            }
        }
        throw new IllegalStateException("SavedData.Factory has no compatible constructor.");
    }

    private static Object staticFieldAssignableTo(Class<?> type, String preferredName) {
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType()) &&
                field.getName().equals(preferredName)) {
                try {
                    return field.get(null);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Could not access " + type.getName() + '.' + preferredName, exception);
                }
            }
        }
        return null;
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not load saved data.", exception);
        }
    }
}
