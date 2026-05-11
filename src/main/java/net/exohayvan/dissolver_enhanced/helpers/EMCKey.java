package net.exohayvan.dissolver_enhanced.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

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

        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            lines.add("Potion: " + potionId(potionContents).orElse("custom"));
            if (!potionContents.customEffects().isEmpty()) {
                lines.add("Custom Potion Effects: " + potionContents.customEffects().size());
            }
        }

        ItemEnchantments storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchantments != null && !storedEnchantments.isEmpty()) {
            lines.add("Stored Enchantments: " + formatEnchantments(storedEnchantments));
        }

        return lines;
    }

    private static String potionKey(ItemStack stack, String itemId) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION) &&
            !stack.is(Items.TIPPED_ARROW)) {
            return null;
        }

        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) {
            return null;
        }

        Optional<String> potionId = potionId(potionContents);
        if (potionId.isEmpty()) {
            return null;
        }

        return itemId + "|potion=" + potionId.get();
    }

    private static Optional<String> potionId(PotionContents potionContents) {
        return potionContents
            .potion()
            .flatMap(Holder<Potion>::unwrapKey)
            .map(key -> key.identifier().toString());
    }

    private static String storedEnchantmentsKey(ItemStack stack, String itemId) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return null;
        }

        ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return null;
        }

        return itemId + "|ench=" + formatEnchantments(enchantments);
    }

    private static String formatEnchantments(ItemEnchantments enchantments) {
        return enchantments
            .entrySet()
            .stream()
            .map(EMCKey::formatEnchantment)
            .sorted(Comparator.naturalOrder())
            .reduce((left, right) -> left + "," + right)
            .orElse("none");
    }

    private static String formatEnchantment(Object2IntMap.Entry<Holder<Enchantment>> entry) {
        String enchantmentId = entry
            .getKey()
            .unwrapKey()
            .map(key -> key.identifier().toString())
            .orElse("unknown");

        return enchantmentId + ":" + entry.getIntValue();
    }

    private static String getItemId(ItemStack stack) {
        return stack.getItem().toString();
    }
}
