package net.exohayvan.dissolver_enhanced.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DissolverInventory extends BaseDissolverInventory {
    public DissolverInventory(AbstractContainerMenu handler, int width, int height) {
        super(handler, width, height);
    }

    public DissolverInventory(
        AbstractContainerMenu handler,
        int width,
        int height,
        NonNullList<ItemStack> stacks
    ) {
        super(handler, width, height, stacks);
    }
}
