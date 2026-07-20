package net.exohayvan.dissolver_enhanced.inventory;

import java.util.function.Predicate;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

class FilteredSlot extends Slot {
    private final Predicate<ItemStack> filter;

    FilteredSlot(Inventory inventory, int index, int x, int y, Predicate<ItemStack> filter) {
        super(inventory, index, x, y);
        this.filter = filter;
    }

    @Override
    public final boolean canInsert(ItemStack stack) {
        return filter.test(stack);
    }
}
