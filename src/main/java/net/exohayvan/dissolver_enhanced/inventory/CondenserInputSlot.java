package net.exohayvan.dissolver_enhanced.inventory;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class CondenserInputSlot extends Slot {
    public CondenserInputSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack) ? EMCOrbItem.getEMC(stack) > 0 : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }
}
