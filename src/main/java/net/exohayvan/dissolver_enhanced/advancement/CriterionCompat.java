package net.exohayvan.dissolver_enhanced.advancement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public final class CriterionCompat {
    private static final String ADAPTER_PACKAGE =
        "net.exohayvan.dissolver_enhanced.advancement.compat.";
    private static final List<PendingCriterion> DEFERRED_CRITERIA = new ArrayList<>();

    private CriterionCompat() {
    }

    public static Object createAndRegister(String id, String criterionClass) {
        String adapterGeneration = adapterGeneration();
        Object trigger = create(criterionClass, adapterGeneration);
        if ("1203".equals(adapterGeneration)) {
            DEFERRED_CRITERIA.add(new PendingCriterion(id, trigger));
            return trigger;
        }
        return register(id, trigger);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerDeferred(IEventBus eventBus) {
        if (DEFERRED_CRITERIA.isEmpty()) {
            return;
        }
        ResourceKey registryKey = createRegistryKey(resourceLocation("minecraft", "trigger_type"));
        DeferredRegister deferredRegister = DeferredRegister.create(registryKey, "dissolver_enhanced");
        for (PendingCriterion pending : DEFERRED_CRITERIA) {
            String path = pending.id().substring(pending.id().indexOf(':') + 1);
            deferredRegister.register(path, pending::trigger);
        }
        deferredRegister.register(eventBus);
    }

    public static void trigger(Object criterion, ServerPlayer player, Object... values) {
        Object[] arguments = new Object[values.length + 1];
        arguments[0] = player;
        System.arraycopy(values, 0, arguments, 1, values.length);

        Method method = findCompatibleMethod(criterion.getClass(), "trigger", arguments);
        if (method == null) {
            throw new IllegalStateException("Criterion " + criterion.getClass().getName() + " has no compatible trigger method.");
        }
        invoke(method, criterion, arguments);
    }

    private static Object create(String criterionClass, String adapterGeneration) {
        String className = adapterGeneration == null
            ? "net.exohayvan.dissolver_enhanced.advancement." + criterionClass
            : ADAPTER_PACKAGE + 'v' + adapterGeneration + '.' + criterionClass + adapterGeneration;

        try {
            return Class.forName(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not load criterion compatibility adapter " + className, exception);
        }
    }

    private static String adapterGeneration() {
        for (Method method : CriterionTrigger.class.getMethods()) {
            if (method.getParameterCount() == 0 &&
                method.getReturnType().getName().equals("com.mojang.serialization.Codec")) {
                return "1203";
            }
        }
        for (Method method : CriteriaTriggers.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) &&
                parameters.length == 2 &&
                parameters[0] == String.class) {
                return "1202";
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T register(String id, T trigger) {
        for (Method method : CriteriaTriggers.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) ||
                !method.getReturnType().isInstance(trigger)) {
                continue;
            }

            Object[] arguments = registrationArguments(method, id, trigger);
            if (arguments == null) {
                continue;
            }
            return (T) invoke(method, null, arguments);
        }

        throw new IllegalStateException("CriteriaTriggers has no compatible registration method for " + id);
    }

    private static Object[] registrationArguments(Method method, String id, Object trigger) {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 1 && parameters[0].isInstance(trigger)) {
            return new Object[] {trigger};
        }
        if (parameters.length == 2 &&
            parameters[0] == String.class &&
            parameters[1].isInstance(trigger)) {
            return new Object[] {id, trigger};
        }
        return null;
    }

    private static Method findCompatibleMethod(Class<?> type, String name, Object[] arguments) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != arguments.length) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameters.length; index++) {
                if (!isCompatible(parameters[index], arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> parameter, Object argument) {
        if (argument == null) {
            return !parameter.isPrimitive();
        }
        if (!parameter.isPrimitive()) {
            return parameter.isInstance(argument);
        }
        return (parameter == int.class && argument instanceof Integer) ||
            (parameter == boolean.class && argument instanceof Boolean);
    }

    private static ResourceLocation resourceLocation(String namespace, String path) {
        try {
            Constructor<ResourceLocation> constructor = ResourceLocation.class.getDeclaredConstructor(String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(namespace, path);
        } catch (ReflectiveOperationException ignored) {
            for (Method method : ResourceLocation.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != ResourceLocation.class) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 2 && parameters[0] == String.class && parameters[1] == String.class) {
                    return (ResourceLocation) invoke(method, null, namespace, path);
                }
            }
            throw new IllegalStateException("Could not construct a ResourceLocation.");
        }
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<? extends Registry<CriterionTrigger<?>>> createRegistryKey(ResourceLocation location) {
        for (Method method : ResourceKey.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1 &&
                method.getParameterTypes()[0] == ResourceLocation.class && method.getReturnType() == ResourceKey.class) {
                return (ResourceKey<? extends Registry<CriterionTrigger<?>>>) invoke(method, null, location);
            }
        }
        throw new IllegalStateException("Could not construct the criterion registry key.");
    }

    private record PendingCriterion(String id, Object trigger) {
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not invoke " + method.getDeclaringClass().getName() + '.' + method.getName(), exception);
        }
    }
}
