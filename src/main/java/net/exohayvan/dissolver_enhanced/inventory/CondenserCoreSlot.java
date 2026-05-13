package net.exohayvan.dissolver_enhanced.inventory;

import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class CondenserCoreSlot extends Slot {
    public CondenserCoreSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return EmcCoreItem.isEmcCore(stack);
    }
}
