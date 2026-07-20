package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
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

public class MaterializerScreenHandler extends MachineScreenHandler {
    public static final int TARGET_SLOT = 0;
    public static final int EMC_INPUT_SLOT = 1;
    public static final int CORE_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int SLOT_COUNT = 4;

    public MaterializerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SLOT_COUNT), new ArrayPropertyDelegate(5));
    }

    public MaterializerScreenHandler(
        int syncId,
        PlayerInventory playerInventory,
        Inventory inventory,
        PropertyDelegate propertyDelegate
    ) {
        super(
            ModScreenHandlers.MATERIALIZER_SCREEN_HANDLER_TYPE,
            syncId,
            playerInventory,
            inventory,
            propertyDelegate,
            SLOT_COUNT
        );
        addSlot(new MaterializerTemplateSlot(inventory, TARGET_SLOT, 56, 17));
        addSlot(new MaterializerTemplateSlot(inventory, EMC_INPUT_SLOT, 38, 53));
        addSlot(new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53));
        addSlot(new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35));
        finishInitialization(playerInventory);
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
            ? EmcCoreItem.getEmcPerSecond(inventory.getStack(CORE_SLOT))
            : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return quickMoveMachineStack(
            player,
            invSlot,
            OUTPUT_SLOT,
            this::moveFromPlayerInventory,
            stack -> { }
        );
    }

    private boolean moveFromPlayerInventory(int invSlot, ItemStack stack) {
        if (EmcCoreItem.isEmcCore(stack)) {
            return insertItem(stack, CORE_SLOT, CORE_SLOT + 1, false);
        }
        if (isMaterializableTarget(stack)) {
            return insertItem(stack, TARGET_SLOT, TARGET_SLOT + 1, false)
                || insertItem(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        }
        if (isEmcInput(stack)) {
            return insertItem(stack, EMC_INPUT_SLOT, EMC_INPUT_SLOT + 1, false);
        }
        return moveBetweenPlayerInventorySections(invSlot, stack);
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
