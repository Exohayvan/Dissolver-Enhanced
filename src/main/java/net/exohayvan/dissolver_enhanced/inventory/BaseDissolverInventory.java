package net.exohayvan.dissolver_enhanced.inventory;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

abstract class BaseDissolverInventory implements Container {
    protected final NonNullList<ItemStack> stacks;
    private final int width;
    private final int height;
    private final AbstractContainerMenu menu;

    protected BaseDissolverInventory(AbstractContainerMenu menu, int width, int height) {
        this(menu, width, height, NonNullList.withSize(width * height, ItemStack.EMPTY));
    }

    protected BaseDissolverInventory(
        AbstractContainerMenu menu,
        int width,
        int height,
        NonNullList<ItemStack> stacks
    ) {
        this.menu = menu;
        this.width = width;
        this.height = height;
        this.stacks = stacks;
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= getContainerSize() ? ItemStack.EMPTY : stacks.get(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = ContainerHelper.removeItem(stacks, slot, amount);
        if (!itemStack.isEmpty()) {
            menu.slotsChanged(this);
        }
        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        menu.slotsChanged(this);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        stacks.clear();
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public List<ItemStack> getHeldStacks() {
        return List.copyOf(stacks);
    }
}
