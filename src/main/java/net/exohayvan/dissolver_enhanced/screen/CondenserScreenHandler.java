package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.inventory.CondenserCoreSlot;
import net.exohayvan.dissolver_enhanced.inventory.CondenserInputSlot;
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

public class CondenserScreenHandler extends MachineScreenHandler {
    public static final int INPUT_SLOT = 0;
    public static final int CORE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = 3;

    public CondenserScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SLOT_COUNT), new ArrayPropertyDelegate(2));
    }

    public CondenserScreenHandler(
        int syncId,
        PlayerInventory playerInventory,
        Inventory inventory,
        PropertyDelegate propertyDelegate
    ) {
        super(
            ModScreenHandlers.CONDENSER_SCREEN_HANDLER_TYPE,
            syncId,
            playerInventory,
            inventory,
            propertyDelegate,
            SLOT_COUNT
        );
        addSlot(new CondenserInputSlot(inventory, INPUT_SLOT, 56, 17));
        addSlot(new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53));
        addSlot(new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35) {
            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                triggerOrbAdvancement(player, stack);
                super.onTakeItem(player, stack);
            }
        });
        finishInitialization(playerInventory);
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        return maxProgress != 0 && progress != 0 ? progress * 24 / maxProgress : 0;
    }

    public BigInteger getCondensingRatePerSecond() {
        ItemStack input = inventory.getStack(INPUT_SLOT);
        BigInteger inputEmc = getInputEmcBig(input);
        if (inputEmc.signum() <= 0) return BigInteger.ZERO;
        if (EMCOrbItem.isEMCOrb(input)) return inputEmc;
        return BigInteger.valueOf(EmcCoreItem.getEmcPerSecond(inventory.getStack(CORE_SLOT)));
    }

    public BigInteger getStoredEmc() {
        ItemStack output = inventory.getStack(OUTPUT_SLOT);
        return EMCOrbItem.isEMCOrb(output) ? EMCOrbItem.getEmcBig(output) : BigInteger.ZERO;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return quickMoveMachineStack(
            player,
            invSlot,
            OUTPUT_SLOT,
            this::moveFromPlayerInventory,
            stack -> triggerOrbAdvancement(player, stack)
        );
    }

    private boolean moveFromPlayerInventory(int invSlot, ItemStack stack) {
        if (EmcCoreItem.isEmcCore(stack)) {
            return insertItem(stack, CORE_SLOT, CORE_SLOT + 1, false);
        }
        if (isCondensable(stack)) {
            return insertItem(stack, INPUT_SLOT, INPUT_SLOT + 1, false);
        }
        return moveBetweenPlayerInventorySections(invSlot, stack);
    }

    private boolean isCondensable(ItemStack stack) {
        return getInputEmcBig(stack).signum() > 0;
    }

    private BigInteger getInputEmcBig(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack)
            ? EMCOrbItem.getEmcBig(stack)
            : BigInteger.valueOf(EMCValues.get(EMCKey.fromStack(stack)));
    }

    private static void triggerOrbAdvancement(PlayerEntity player, ItemStack stack) {
        if (EMCOrbItem.isEMCOrb(stack)) {
            ModCriteria.triggerEmcOrb(player, EMCOrbItem.getEmcBig(stack), "created");
        }
    }
}
