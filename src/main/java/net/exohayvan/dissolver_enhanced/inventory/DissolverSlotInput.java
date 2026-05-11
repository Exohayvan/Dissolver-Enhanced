package net.exohayvan.dissolver_enhanced.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DissolverSlotInput extends Slot {
    public int id;

    public DissolverSlotInput(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    public ItemStack insertStack(ItemStack stack, int count) {
        if (stack.isEmpty() || !this.mayPlace(stack)) return stack;

        ItemStack itemStack = this.getItem();
        int i = Math.min(Math.min(count, stack.getCount()), this.getMaxStackSize(stack) - itemStack.getCount());

        if (itemStack.isEmpty()) {
            this.set(stack.split(i));
        } else if (ItemStack.isSameItemSameTags(itemStack, stack)) {
            stack.shrink(i);
            itemStack.grow(i);
            this.set(itemStack);
        }

        return stack;
    }
}
