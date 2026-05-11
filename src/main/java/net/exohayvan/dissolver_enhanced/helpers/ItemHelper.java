package net.exohayvan.dissolver_enhanced.helpers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemHelper {
    public static Item getById(String itemId) {
        return BuiltInRegistries.ITEM.getValue(identifierById(itemId));
    }

    public static String getName(Item item) {
        return item.getName(item.getDefaultInstance()).getString();
    }

    public static double getDurabilityPercentage(ItemStack stack) {
        // reduce EMC value based on current item durability
        float MAX_DURABILITY = stack.getMaxDamage();
        float CURRENT_DURABILITY = MAX_DURABILITY - stack.getDamageValue();
        return MAX_DURABILITY == 0 ? 1 : CURRENT_DURABILITY / MAX_DURABILITY;
    }

    // HELPERS

    public static Identifier identifierById(String fullItemId) {
        fullItemId = EMCKey.baseItemId(fullItemId);
        String[] parts = fullItemId.split(":");
        String modId = parts[0];
        String itemId = parts[1];

        return Identifier.fromNamespaceAndPath(modId, itemId);
    }
}
