package net.exohayvan.dissolver_enhanced.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

public class EMCKey {
    public static String fromStack(ItemStack stack) {
        String itemId = getItemId(stack);

        String potionKey = potionKey(stack, itemId);
        if (potionKey != null) {
            return potionKey;
        }

        String enchantmentKey = storedEnchantmentsKey(stack, itemId);
        if (enchantmentKey != null) {
            return enchantmentKey;
        }

        return itemId;
    }

    public static String baseItemId(String key) {
        int componentIndex = key.indexOf("|");
        return componentIndex == -1 ? key : key.substring(0, componentIndex);
    }

    public static boolean isComponentKey(String key) {
        return key.indexOf("|") != -1;
    }

    public static List<String> describe(ItemStack stack) {
        List<String> lines = new ArrayList<>();

        PotionContentsComponent potionContents = getComponent(stack, DataComponentTypes.POTION_CONTENTS);
        if (potionContents != null) {
            lines.add("Potion: " + potionId(potionContents).orElse("custom"));
            if (!potionContents.customEffects().isEmpty()) {
                lines.add("Custom Potion Effects: " + potionContents.customEffects().size());
            }
        }

        ItemEnchantmentsComponent storedEnchantments = getComponent(stack, DataComponentTypes.STORED_ENCHANTMENTS);
        if (storedEnchantments != null && !storedEnchantments.isEmpty()) {
            lines.add("Stored Enchantments: " + formatEnchantments(storedEnchantments));
        }

        return lines;
    }

    private static String potionKey(ItemStack stack, String itemId) {
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION) &&
            !stack.isOf(Items.TIPPED_ARROW)) {
            return null;
        }

        PotionContentsComponent potionContents = getComponent(stack, DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            return null;
        }

        Optional<String> potionId = potionId(potionContents);
        if (potionId.isEmpty()) {
            return null;
        }

        return itemId + "|potion=" + potionId.get();
    }

    private static Optional<String> potionId(PotionContentsComponent potionContents) {
        return potionContents
            .potion()
            .flatMap(RegistryEntry<Potion>::getKey)
            .map(key -> key.getValue().toString());
    }

    private static String storedEnchantmentsKey(ItemStack stack, String itemId) {
        if (!stack.isOf(Items.ENCHANTED_BOOK)) {
            return null;
        }

        ItemEnchantmentsComponent enchantments = getComponent(stack, DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return null;
        }

        return itemId + "|ench=" + formatEnchantments(enchantments);
    }

    private static String formatEnchantments(ItemEnchantmentsComponent enchantments) {
        return enchantments
            .getEnchantmentEntries()
            .stream()
            .map(EMCKey::formatEnchantment)
            .sorted(Comparator.naturalOrder())
            .reduce((left, right) -> left + "," + right)
            .orElse("none");
    }

    private static String formatEnchantment(Object2IntMap.Entry<RegistryEntry<Enchantment>> entry) {
        String enchantmentId = entry
            .getKey()
            .getKey()
            .map(key -> key.getValue().toString())
            .orElse("unknown");

        return enchantmentId + ":" + entry.getIntValue();
    }

    private static String getItemId(ItemStack stack) {
        return stack.getItem().toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getComponent(ItemStack stack, ComponentType<T> componentType) {
        Method method = findComponentGetter(stack.getClass());
        if (method != null) {
            try {
                return (T) method.invoke(stack, componentType);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to read item component " + componentType, exception);
            }
        }

        throw new IllegalStateException("No supported ItemStack component getter found");
    }

    private static Method findComponentGetter(Class<?> owner) {
        Method modernGetter = findComponentGetter(owner, "method_58694");
        if (modernGetter != null) {
            return modernGetter;
        }

        String[] fallbackNames = {"get", "method_57824", "method_57381"};
        for (String name : fallbackNames) {
            Method method = findComponentGetter(owner, name);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    private static Method findComponentGetter(Class<?> owner, String name) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && isComponentGetter(method)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isComponentGetter(Method method) {
        if (method.getParameterCount() != 1) {
            return false;
        }

        return method.getParameterTypes()[0].isAssignableFrom(ComponentType.class) ||
                method.getParameterTypes()[0].isInterface();
    }
}
