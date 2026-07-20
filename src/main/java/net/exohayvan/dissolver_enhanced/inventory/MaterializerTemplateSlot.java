package net.exohayvan.dissolver_enhanced.inventory;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.minecraft.inventory.Inventory;

public class MaterializerTemplateSlot extends FilteredSlot {
    public MaterializerTemplateSlot(Inventory inventory, int index, int x, int y) {
        super(
            inventory,
            index,
            x,
            y,
            stack -> !EMCOrbItem.isEMCOrb(stack) && EMCValues.get(EMCKey.fromStack(stack)) > 0
        );
    }
}
