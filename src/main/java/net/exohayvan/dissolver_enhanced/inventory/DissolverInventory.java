package net.exohayvan.dissolver_enhanced.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;

public class DissolverInventory extends BaseDissolverInventory<ScreenHandler> {
    public DissolverInventory(ScreenHandler handler, int width, int height) {
        this(handler, width, height, DefaultedList.ofSize(width * height, ItemStack.EMPTY));
    }

    public DissolverInventory(
        ScreenHandler handler,
        int width,
        int height,
        DefaultedList<ItemStack> stacks
    ) {
        super(handler, width, height, stacks);
    }
}
