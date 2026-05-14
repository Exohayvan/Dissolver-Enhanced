package net.exohayvan.dissolver_enhanced.helpers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemHelper {
    public static Item getById(String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(identifierById(itemId));
        return item == null ? Items.AIR : item;
    }

    public static String getId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? item.toString() : id.toString();
    }

    public static String getName(Item item) {
        return item.getDescription().getString();
    }

    public static double getDurabilityPercentage(ItemStack stack) {
        // reduce EMC value based on current item durability
        float MAX_DURABILITY = stack.getMaxDamage();
        float CURRENT_DURABILITY = MAX_DURABILITY - stack.getDamageValue();
        return MAX_DURABILITY == 0 ? 1 : CURRENT_DURABILITY / MAX_DURABILITY;
    }

    // HELPERS

    public static ResourceLocation identifierById(String fullItemId) {
        fullItemId = EMCKey.baseItemId(fullItemId);
        String[] parts = fullItemId.split(":");
        String modId = parts.length > 1 ? parts[0] : "minecraft";
        String itemId = parts.length > 1 ? parts[1] : parts[0];

        return new ResourceLocation(modId, itemId);
    }
}
