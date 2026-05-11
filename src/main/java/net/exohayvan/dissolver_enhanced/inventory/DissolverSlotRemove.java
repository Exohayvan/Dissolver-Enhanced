package net.exohayvan.dissolver_enhanced.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class DissolverSlotRemove extends Slot {
    public int id;

    public DissolverSlotRemove(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    // public ItemStack insertStack(ItemStack stack, int count) {
    //     if (stack.isEmpty() || !this.canInsert(stack)) return stack;

    //     String itemId = stack.getItem().toString();
    //     EMCHelper.learnItem(player, itemId);

    //     return stack;
    // }
}
