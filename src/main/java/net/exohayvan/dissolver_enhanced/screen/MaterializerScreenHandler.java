package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.inventory.CondenserCoreSlot;
import net.exohayvan.dissolver_enhanced.inventory.MaterializerTemplateSlot;
import net.exohayvan.dissolver_enhanced.inventory.OutputOnlySlot;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;

import net.minecraft.world.inventory.Slot;

public class MaterializerScreenHandler extends MachineScreenHandler {
    public static final int TARGET_SLOT = 0;
    public static final int EMC_INPUT_SLOT = 1;
    public static final int CORE_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int MATERIALIZER_SLOT_COUNT = 4;

    public MaterializerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MATERIALIZER_SLOT_COUNT), new SimpleContainerData(5));
    }

    public MaterializerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE.get(), syncId, playerInventory, inventory, propertyDelegate, MATERIALIZER_SLOT_COUNT);

        this.addSlot(new MaterializerTemplateSlot(inventory, TARGET_SLOT, 56, 17));
        this.addSlot(new MaterializerTemplateSlot(inventory, EMC_INPUT_SLOT, 38, 53));
        this.addSlot(new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53));
        this.addSlot(new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35));

        finishSetup(playerInventory);
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
        return getTargetValue() > 0 && getInputValue() > 0 ? EmcCoreItem.getEmcPerSecond(this.inventory.getItem(CORE_SLOT)) : 0;
    }

    protected int outputSlot() {
        return OUTPUT_SLOT;
    }

    @Override
    protected boolean movePlayerStack(ItemStack stack, int slotIndex) {
        if (EmcCoreItem.isEmcCore(stack)) return moveItemStackTo(stack, CORE_SLOT, CORE_SLOT + 1, false);
        if (isMaterializableTarget(stack)) {
            return moveItemStackTo(stack, TARGET_SLOT, TARGET_SLOT + 1, false)
                || moveItemStackTo(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        }
        if (isEmcInput(stack)) return moveItemStackTo(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        return moveBetweenPlayerInventoryAndHotbar(stack, slotIndex);
    }

    private boolean isMaterializableTarget(ItemStack stack) {
        return !EMCOrbItem.isEMCOrb(stack) && EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    private boolean isEmcInput(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack) ? EMCOrbItem.getEMC(stack) > 0 : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

}
