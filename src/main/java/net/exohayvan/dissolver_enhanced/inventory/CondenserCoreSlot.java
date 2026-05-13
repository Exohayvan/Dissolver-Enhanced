package net.exohayvan.dissolver_enhanced.inventory;

import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class CondenserCoreSlot extends Slot {
    public CondenserCoreSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return EmcCoreItem.isEmcCore(stack);
    }
}
