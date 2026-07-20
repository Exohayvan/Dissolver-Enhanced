package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReflectionCompat {
    private ReflectionCompat() {
    }

    static <T> T invokeNoArg(Object target, Class<T> resultType, String... methodNames) {
        for (String methodName : methodNames) {
            Method method = findNoArgMethod(target.getClass(), methodName);
            if (method == null || !resultType.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                method.setAccessible(true);
                return resultType.cast(method.invoke(target));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    static <T> T readField(Object target, Class<T> fieldType) {
        Class<?> owner = target.getClass();
        while (owner != null) {
            for (Field field : owner.getDeclaredFields()) {
                if (!fieldType.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    return fieldType.cast(field.get(target));
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
