package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.inventory.CondenserCoreSlot;
import net.exohayvan.dissolver_enhanced.inventory.MaterializerTemplateSlot;
import net.exohayvan.dissolver_enhanced.inventory.OutputOnlySlot;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class MaterializerScreenHandler extends MachineScreenHandler {
    public static final int TARGET_SLOT = 0;
    public static final int EMC_INPUT_SLOT = 1;
    public static final int CORE_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int SLOT_COUNT = 4;

    public MaterializerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(5));
    }

    public MaterializerScreenHandler(
        int syncId,
        Inventory playerInventory,
        Container inventory,
        ContainerData propertyDelegate
    ) {
        super(
            ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE,
            syncId,
            playerInventory,
            inventory,
            propertyDelegate,
            SLOT_COUNT
        );
        completeSetup(
            playerInventory,
            new MaterializerTemplateSlot(inventory, TARGET_SLOT, 56, 17),
            new MaterializerTemplateSlot(inventory, EMC_INPUT_SLOT, 38, 53),
            new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53),
            new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35)
        );
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        return maxProgress > 0 && progress > 0 ? Math.min(24, progress * 24 / maxProgress) : 0;
    }

    public int getStoredEmc() {
        return propertyDelegate.get(2);
    }

    public int getTargetValue() {
        return propertyDelegate.get(3);
    }

    public int getInputValue() {
        return propertyDelegate.get(4);
    }

    public int getMaterializingRatePerSecond() {
        return getTargetValue() > 0 && getInputValue() > 0
            ? EmcCoreItem.getEmcPerSecond(inventory.getItem(CORE_SLOT))
            : 0;
    }

    @Override
    protected int outputSlot() {
        return OUTPUT_SLOT;
    }

    @Override
    protected boolean movePlayerItem(ItemStack stack, int invSlot) {
        if (EmcCoreItem.isEmcCore(stack)) {
            return moveItemStackTo(stack, CORE_SLOT, CORE_SLOT + 1, false);
        }
        if (isMaterializableTarget(stack)) {
            return moveItemStackTo(stack, TARGET_SLOT, TARGET_SLOT + 1, false)
                || moveItemStackTo(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        }
        if (isEmcInput(stack)) {
            return moveItemStackTo(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        }
        if (invSlot < inventoryEnd()) {
            return moveItemStackTo(stack, hotbarStart(), hotbarEnd(), false);
        }
        return moveItemStackTo(stack, inventoryStart(), inventoryEnd(), false);
    }

    private boolean isMaterializableTarget(ItemStack stack) {
        return !EMCOrbItem.isEMCOrb(stack) && EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    private boolean isEmcInput(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack)
            ? EMCOrbItem.getEMC(stack) > 0
            : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }
}
