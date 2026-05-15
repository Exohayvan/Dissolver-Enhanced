package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.util.ActionResult;

public final class ActionResultCompat {
    private ActionResultCompat() {
    }

    public static ActionResult success(boolean client) {
        ActionResult result = invokeBooleanFactory(client);
        if (result != null) return result;

        result = namedResult("SUCCESS", "field_5812", "SUCCESS_NO_ITEM_USED", "field_51370");
        return result != null ? result : acceptedResult();
    }

    public static ActionResult consume() {
        ActionResult result = namedResult("CONSUME", "field_21466");
        if (result != null) return result;

        result = invokeBooleanFactory(false);
        return result != null ? result : acceptedResult();
    }

    public static ActionResult pass() {
        ActionResult result = namedResult("PASS", "field_5811");
        return result != null ? result : firstStaticResult();
    }

    private static ActionResult invokeBooleanFactory(boolean client) {
        for (Method method : ActionResult.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1 || method.getParameterTypes()[0] != boolean.class) {
                continue;
            }

            try {
                method.setAccessible(true);
                Object result = method.invoke(null, client);
                if (result instanceof ActionResult actionResult) {
                    return actionResult;
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Try the next compatible factory.
            }
        }

        return null;
    }

    private static ActionResult namedResult(String... names) {
        for (String name : names) {
            ActionResult result = fieldResult(name);
            if (result != null) return result;
        }

        return null;
    }

    private static ActionResult fieldResult(String name) {
        try {
            Field field = ActionResult.class.getDeclaredField(name);
            field.setAccessible(true);
            Object result = field.get(null);
            return result instanceof ActionResult actionResult ? actionResult : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static ActionResult acceptedResult() {
        for (ActionResult result : staticResults()) {
            if (isAccepted(result)) {
                return result;
            }
        }

        return firstStaticResult();
    }

    private static boolean isAccepted(ActionResult result) {
        try {
            Method method = ActionResult.class.getDeclaredMethod("method_23665");
            method.setAccessible(true);
            return (boolean)method.invoke(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return true;
        }
    }

    private static ActionResult[] staticResults() {
        return java.util.Arrays.stream(ActionResult.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .map(field -> {
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    return value instanceof ActionResult actionResult ? actionResult : null;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return null;
                }
            })
            .filter(result -> result != null)
            .toArray(ActionResult[]::new);
    }

    private static ActionResult firstStaticResult() {
        ActionResult[] values = staticResults();
        if (values.length > 0) {
            return values[0];
        }

        ActionResult result = invokeBooleanFactory(true);
        if (result != null) {
            return result;
        }

        throw new IllegalStateException("ActionResult has no compatible runtime values.");
    }
}
