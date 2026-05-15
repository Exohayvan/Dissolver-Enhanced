package net.exohayvan.dissolver_enhanced.helpers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class EMCKey {
    public static String fromStack(ItemStack stack) {
        return getItemId(stack);
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
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.isEmpty()) {
            lines.add("NBT: " + customData);
        }
        return lines;
    }

    private static String getItemId(ItemStack stack) {
        return ItemHelper.getId(stack.getItem());
    }
}
