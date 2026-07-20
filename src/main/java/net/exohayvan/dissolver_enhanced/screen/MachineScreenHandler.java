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
    private final int inventoryStart;
    private final int inventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    protected MachineScreenHandler(
        MenuType<?> type,
        int syncId,
        Inventory playerInventory,
        Container inventory,
        ContainerData propertyDelegate,
        int machineSlotCount
    ) {
        super(type, syncId);
        checkContainerSize(inventory, machineSlotCount);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.inventoryStart = machineSlotCount;
        this.inventoryEnd = inventoryStart + 27;
        this.hotbarStart = inventoryEnd;
        this.hotbarEnd = hotbarStart + 9;
        inventory.startOpen(playerInventory.player);
    }

    protected final void completeSetup(Inventory playerInventory, Slot... machineSlots) {
        for (Slot machineSlot : machineSlots) {
            addSlot(machineSlot);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                    playerInventory,
                    column + row * 9 + 9,
                    8 + column * 18,
                    84 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(propertyDelegate);
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int invSlot) {
        Slot slot = slots.get(invSlot);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getItem();
        ItemStack newStack = originalStack.copy();
        if (invSlot == outputSlot()) {
            onOutputTaken(player, originalStack);
            if (!moveItemStackTo(originalStack, inventoryStart, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(originalStack, newStack);
        } else if (invSlot >= inventoryStart && invSlot < hotbarEnd) {
            if (!movePlayerItem(originalStack, invSlot)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(originalStack, inventoryStart, hotbarEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return newStack;
    }

    protected abstract int outputSlot();

    protected abstract boolean movePlayerItem(ItemStack stack, int invSlot);

    protected void onOutputTaken(Player player, ItemStack stack) {
    }

    protected final int inventoryStart() {
        return inventoryStart;
    }

    protected final int inventoryEnd() {
        return inventoryEnd;
    }

    protected final int hotbarStart() {
        return hotbarStart;
    }

    protected final int hotbarEnd() {
        return hotbarEnd;
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
