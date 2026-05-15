package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.InventoryCompat;
import net.exohayvan.dissolver_enhanced.inventory.CondenserCoreSlot;
import net.exohayvan.dissolver_enhanced.inventory.MaterializerTemplateSlot;
import net.exohayvan.dissolver_enhanced.inventory.OutputOnlySlot;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class MaterializerScreenHandler extends ScreenHandler {
    public static final int TARGET_SLOT = 0;
    public static final int EMC_INPUT_SLOT = 1;
    public static final int CORE_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int MATERIALIZER_SLOT_COUNT = 4;
    private static final int INVENTORY_START = MATERIALIZER_SLOT_COUNT;
    private static final int INVENTORY_END = INVENTORY_START + 27;
    private static final int HOTBAR_START = INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public MaterializerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(MATERIALIZER_SLOT_COUNT), new ArrayPropertyDelegate(5));
    }

    public MaterializerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE, syncId);
        checkSize(inventory, MATERIALIZER_SLOT_COUNT);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        InventoryCompat.onOpen(inventory, playerInventory.player);

        this.addSlot(new MaterializerTemplateSlot(inventory, TARGET_SLOT, 56, 17));
        this.addSlot(new MaterializerTemplateSlot(inventory, EMC_INPUT_SLOT, 38, 53));
        this.addSlot(new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53));
        this.addSlot(new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(propertyDelegate);
    }

    public int getScaledProgress() {
        int progress = this.propertyDelegate.get(0);
        int maxProgress = this.propertyDelegate.get(1);
        return maxProgress > 0 && progress > 0 ? Math.min(24, progress * 24 / maxProgress) : 0;
    }

    public int getStoredEmc() {
        return this.propertyDelegate.get(2);
    }

    public int getTargetValue() {
        return this.propertyDelegate.get(3);
    }

    public int getInputValue() {
        return this.propertyDelegate.get(4);
    }

    public int getMaterializingRatePerSecond() {
        return getTargetValue() > 0 && getInputValue() > 0 ? EmcCoreItem.getEmcPerSecond(this.inventory.getStack(CORE_SLOT)) : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot == null || !slot.hasStack()) return newStack;

        ItemStack originalStack = slot.getStack();
        newStack = originalStack.copy();

        if (invSlot == OUTPUT_SLOT) {
            if (!this.insertItem(originalStack, INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickTransfer(originalStack, newStack);
        } else if (invSlot >= INVENTORY_START && invSlot < HOTBAR_END) {
            if (EmcCoreItem.isEmcCore(originalStack)) {
                if (!this.insertItem(originalStack, CORE_SLOT, CORE_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (isMaterializableTarget(originalStack)) {
                if (!this.insertItem(originalStack, TARGET_SLOT, TARGET_SLOT + 1, false)
                        && !this.insertItem(originalStack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isEmcInput(originalStack)) {
                if (!this.insertItem(originalStack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (invSlot < INVENTORY_END) {
                if (!this.insertItem(originalStack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (!this.insertItem(originalStack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertItem(originalStack, INVENTORY_START, HOTBAR_END, false)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        InventoryCompat.onClose(this.inventory, player);
    }

    private boolean isMaterializableTarget(ItemStack stack) {
        return !EMCOrbItem.isEMCOrb(stack) && EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    private boolean isEmcInput(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack) ? EMCOrbItem.getEMC(stack) > 0 : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
