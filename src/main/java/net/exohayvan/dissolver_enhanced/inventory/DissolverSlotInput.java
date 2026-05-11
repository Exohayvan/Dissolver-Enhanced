package net.exohayvan.dissolver_enhanced.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DissolverSlotInput extends Slot {
    public int id;

    public DissolverSlotInput(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    // @Override
    // public boolean canInsert(ItemStack stack) {
    //     return false;
    // }

    // public void setStack(ItemStack stack) {
    //     this.setStack(stack, this.getStack());
    // }

    // public void setStack(ItemStack stack, ItemStack previousStack) {
    //     this.setStackNoCallbacks(stack);
    // }

    // public ItemStack insertStack(ItemStack stack) {
    //     return this.insertStack(stack, stack.getCount());
    // }

    public ItemStack safeInsert(ItemStack stack, int count) {
        if (stack.isEmpty() || !this.mayPlace(stack)) return stack;

        ItemStack itemStack = this.getItem();
        int i = Math.min(Math.min(count, stack.getCount()), this.getMaxStackSize(stack) - itemStack.getCount());

        if (itemStack.isEmpty()) {
            this.setByPlayer(stack.split(i));
        } else if (ItemStack.isSameItemSameComponents(itemStack, stack)) {
            stack.shrink(i);
            itemStack.grow(i);
            this.setByPlayer(itemStack);
        }

        return stack;
    }
}
