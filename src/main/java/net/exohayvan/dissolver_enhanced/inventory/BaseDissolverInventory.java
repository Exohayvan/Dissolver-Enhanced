package net.exohayvan.dissolver_enhanced.inventory;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;

abstract class BaseDissolverInventory<H extends ScreenHandler> implements Inventory {
    protected final DefaultedList<ItemStack> stacks;
    protected final H handler;
    private final int width;
    private final int height;

    protected BaseDissolverInventory(H handler, int width, int height, DefaultedList<ItemStack> stacks) {
        this.handler = handler;
        this.width = width;
        this.height = height;
        this.stacks = stacks;
    }

    @Override
    public final int size() {
        return stacks.size();
    }

    @Override
    public final boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public final ItemStack getStack(int slot) {
        return slot >= size() ? ItemStack.EMPTY : stacks.get(slot);
    }

    @Override
    public final ItemStack removeStack(int slot) {
        return Inventories.removeStack(stacks, slot);
    }

    @Override
    public final ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.splitStack(stacks, slot, amount);
        if (!stack.isEmpty()) {
            handler.onContentChanged(this);
        }
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        handler.onContentChanged(this);
    }

    @Override
    public final void markDirty() {
    }

    @Override
    public final boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public final void clear() {
        stacks.clear();
    }

    public final int getHeight() {
        return height;
    }

    public final int getWidth() {
        return width;
    }

    public final List<ItemStack> getHeldStacks() {
        return List.copyOf(stacks);
    }
}
