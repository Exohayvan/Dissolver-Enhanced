package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemHelper {
    public static Item getById(String itemId) {
        Identifier identifier = identifierById(itemId);
        Object registry = Registries.ITEM;

        for (Method method : registry.getClass().getMethods()) {
            Item item = tryGetItem(registry, method, identifier);
            if (item != null) {
                return item;
            }
        }

        return null;
    }

    public static String getName(Item item) {
        String textName = getTextName(item);
        if (!textName.isEmpty()) {
            return textName;
        }

        String itemId = getId(item);
        if (!itemId.isEmpty()) {
            return itemId;
        }

        String translationKey = getStringName(item);
        if (!translationKey.isEmpty()) {
            return translationKey;
        }

        return String.valueOf(item);
    }

    public static double getDurabilityPercentage(ItemStack stack) {
        // reduce EMC value based on current item durability
        float MAX_DURABILITY = stack.getMaxDamage();
        float CURRENT_DURABILITY = MAX_DURABILITY - stack.getDamage();
        return MAX_DURABILITY == 0 ? 1 : CURRENT_DURABILITY / MAX_DURABILITY;
    }

    // HELPERS

    public static Identifier identifierById(String fullItemId) {
        fullItemId = EMCKey.baseItemId(fullItemId);
        String[] parts = fullItemId.split(":");
        String modId = parts[0];
        String itemId = parts[1];

        return Identifier.of(modId, itemId);
    }

    private static Item tryGetItem(Object registry, Method method, Identifier identifier) {
        if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(Identifier.class)) {
            return null;
        }

        try {
            method.setAccessible(true);
            Object result = method.invoke(registry, identifier);
            return asItem(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Item asItem(Object result) {
        if (result instanceof Item item) {
            return item;
        }

        if (result instanceof Optional<?> optional && optional.isPresent()) {
            return asItem(optional.get());
        }

        if (result == null) {
            return null;
        }

        try {
            Method valueMethod = result.getClass().getMethod("comp_349");
            valueMethod.setAccessible(true);
            Object value = valueMethod.invoke(result);
            return value instanceof Item item ? item : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static String getTextName(Item item) {
        List<String> preferredNames = Arrays.asList("getName", "method_7848");

        for (String methodName : preferredNames) {
            String name = invokeTextMethod(item, methodName);
            if (!name.isEmpty()) {
                return name;
            }
        }

        for (Method method : item.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !Text.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            String name = invokeTextMethod(item, method);
            if (!name.isEmpty()) {
                return name;
            }
        }

        return "";
    }

    private static String invokeTextMethod(Item item, String methodName) {
        try {
            Method method = item.getClass().getMethod(methodName);
            return invokeTextMethod(item, method);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }

    private static String invokeTextMethod(Item item, Method method) {
        try {
            method.setAccessible(true);
            Object text = method.invoke(item);
            return textToString(text);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }

    private static String textToString(Object text) {
        if (text == null) {
            return "";
        }

        List<String> preferredNames = Arrays.asList("getString", "method_10851");
        for (String methodName : preferredNames) {
            try {
                Method method = text.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                    method.setAccessible(true);
                    Object result = method.invoke(text);
                    return result instanceof String name ? name : "";
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Try the next runtime name.
            }
        }

        return String.valueOf(text);
    }

    private static String getId(Item item) {
        Object registry = Registries.ITEM;

        for (Method method : registry.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(item.getClass())
                    || !Identifier.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                method.setAccessible(true);
                Object result = method.invoke(registry, item);
                if (result instanceof Identifier identifier) {
                    return identifier.toString();
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Keep searching for the runtime registry accessor.
            }
        }

        return "";
    }

    private static String getStringName(Item item) {
        List<String> preferredNames = Arrays.asList("getTranslationKey", "method_7876", "method_7869");

        for (String methodName : preferredNames) {
            String name = invokeStringMethod(item, methodName);
            if (!name.isEmpty()) {
                return name;
            }
        }

        for (Method method : item.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class || method.getName().equals("toString")) {
                continue;
            }

            String name = invokeStringMethod(item, method);
            if (!name.isEmpty()) {
                return name;
            }
        }

        return "";
    }

    private static String invokeStringMethod(Item item, String methodName) {
        try {
            Method method = item.getClass().getMethod(methodName);
            return invokeStringMethod(item, method);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }

    private static String invokeStringMethod(Item item, Method method) {
        try {
            method.setAccessible(true);
            Object result = method.invoke(item);
            return result instanceof String name ? name : "";
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }
}
