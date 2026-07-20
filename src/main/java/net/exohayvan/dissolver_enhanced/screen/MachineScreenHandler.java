package net.exohayvan.dissolver_enhanced.screen;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import net.exohayvan.dissolver_enhanced.helpers.InventoryCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

abstract class MachineScreenHandler extends ScreenHandler {
    protected final Inventory inventory;
    protected final PropertyDelegate propertyDelegate;
    protected final int inventoryStart;
    protected final int inventoryEnd;
    protected final int hotbarStart;
    protected final int hotbarEnd;

    protected MachineScreenHandler(
        ScreenHandlerType<?> type,
        int syncId,
        PlayerInventory playerInventory,
        Inventory inventory,
        PropertyDelegate propertyDelegate,
        int machineSlotCount
    ) {
        super(type, syncId);
        checkSize(inventory, machineSlotCount);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.inventoryStart = machineSlotCount;
        this.inventoryEnd = inventoryStart + 27;
        this.hotbarStart = inventoryEnd;
        this.hotbarEnd = hotbarStart + 9;
        InventoryCompat.onOpen(inventory, playerInventory.player);
    }

    protected final void finishInitialization(PlayerInventory playerInventory) {
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
        addProperties(propertyDelegate);
    }

    protected final ItemStack quickMoveMachineStack(
        PlayerEntity player,
        int invSlot,
        int outputSlot,
        BiPredicate<Integer, ItemStack> moveFromPlayerInventory,
        Consumer<ItemStack> onOutput
    ) {
        Slot slot = slots.get(invSlot);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getStack();
        ItemStack copiedStack = originalStack.copy();

        if (invSlot == outputSlot) {
            onOutput.accept(originalStack);
            if (!insertItem(originalStack, inventoryStart, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickTransfer(originalStack, copiedStack);
        } else if (invSlot >= inventoryStart && invSlot < hotbarEnd) {
            if (!moveFromPlayerInventory.test(invSlot, originalStack)) return ItemStack.EMPTY;
        } else if (!insertItem(originalStack, inventoryStart, hotbarEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return copiedStack;
    }

    protected final boolean moveBetweenPlayerInventorySections(int invSlot, ItemStack stack) {
        if (invSlot < inventoryEnd) {
            return insertItem(stack, hotbarStart, hotbarEnd, false);
        }
        return insertItem(stack, inventoryStart, inventoryEnd, false);
    }

    @Override
    public final boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        InventoryCompat.onClose(inventory, player);
    }
}
