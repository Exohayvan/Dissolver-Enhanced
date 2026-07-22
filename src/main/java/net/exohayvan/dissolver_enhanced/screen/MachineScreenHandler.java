package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

abstract class MachineScreenHandler extends AbstractContainerMenu {
    protected final Container inventory;
    protected final ContainerData propertyDelegate;
    protected final int inventoryStart;
    protected final int inventoryEnd;
    protected final int hotbarStart;
    protected final int hotbarEnd;

    protected MachineScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory,
                                   Container inventory, ContainerData propertyDelegate, int machineSlots) {
        super(type, syncId);
        checkContainerSize(inventory, machineSlots);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.inventoryStart = machineSlots;
        this.inventoryEnd = machineSlots + 27;
        this.hotbarStart = this.inventoryEnd;
        this.hotbarEnd = this.hotbarStart + 9;
        inventory.startOpen(playerInventory.player);
    }

    protected final void finishSetup(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(propertyDelegate);
    }

    protected abstract int outputSlot();

    protected abstract boolean movePlayerStack(ItemStack stack, int slotIndex);

    protected void beforeOutputQuickMove(Player player, ItemStack stack) {
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (slotIndex == outputSlot()) {
            beforeOutputQuickMove(player, original);
            if (!moveItemStackTo(original, inventoryStart, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(original, copy);
        } else if (slotIndex >= inventoryStart && slotIndex < hotbarEnd) {
            if (!movePlayerStack(original, slotIndex)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, inventoryStart, hotbarEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    protected final boolean moveBetweenPlayerInventoryAndHotbar(ItemStack stack, int slotIndex) {
        if (slotIndex < inventoryEnd) return moveItemStackTo(stack, hotbarStart, hotbarEnd, false);
        return moveItemStackTo(stack, inventoryStart, inventoryEnd, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }
}