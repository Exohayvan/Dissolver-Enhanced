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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class CondenserScreenHandler extends MachineScreenHandler {
    public static final int INPUT_SLOT = 0;
    public static final int CORE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = 3;

    public CondenserScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(2));
    }

    public CondenserScreenHandler(
        int syncId,
        Inventory playerInventory,
        Container inventory,
        ContainerData propertyDelegate
    ) {
        super(
            ModScreenHandlers.CONDENSER_SCREEN_HANDLER_TYPE,
            syncId,
            playerInventory,
            inventory,
            propertyDelegate,
            SLOT_COUNT
        );
        completeSetup(
            playerInventory,
            new CondenserInputSlot(inventory, INPUT_SLOT, 56, 17),
            new CondenserCoreSlot(inventory, CORE_SLOT, 56, 53),
            new OutputOnlySlot(inventory, OUTPUT_SLOT, 116, 35) {
                @Override
                public void onTake(Player player, ItemStack stack) {
                    triggerOrbAdvancement(player, stack);
                    super.onTake(player, stack);
                }
            }
        );
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        return maxProgress != 0 && progress != 0 ? progress * 24 / maxProgress : 0;
    }

    public BigInteger getCondensingRatePerSecond() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        BigInteger inputEmc = getInputEmcBig(input);
        if (inputEmc.signum() <= 0) return BigInteger.ZERO;
        if (EMCOrbItem.isEMCOrb(input)) return inputEmc;
        return BigInteger.valueOf(EmcCoreItem.getEmcPerSecond(inventory.getItem(CORE_SLOT)));
    }

    public BigInteger getStoredEmc() {
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        return EMCOrbItem.isEMCOrb(output) ? EMCOrbItem.getEmcBig(output) : BigInteger.ZERO;
    }

    @Override
    protected int outputSlot() {
        return OUTPUT_SLOT;
    }

    @Override
    protected void onOutputTaken(Player player, ItemStack stack) {
        triggerOrbAdvancement(player, stack);
    }

    @Override
    protected boolean movePlayerItem(ItemStack stack, int invSlot) {
        if (EmcCoreItem.isEmcCore(stack)) {
            return moveItemStackTo(stack, CORE_SLOT, CORE_SLOT + 1, false);
        }
        if (isCondensable(stack)) {
            return moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false);
        }
        if (invSlot < inventoryEnd()) {
            return moveItemStackTo(stack, hotbarStart(), hotbarEnd(), false);
        }
        return moveItemStackTo(stack, inventoryStart(), inventoryEnd(), false);
    }

    private boolean isCondensable(ItemStack stack) {
        return getInputEmcBig(stack).signum() > 0;
    }

    private BigInteger getInputEmcBig(ItemStack stack) {
        return EMCOrbItem.isEMCOrb(stack)
            ? EMCOrbItem.getEmcBig(stack)
            : BigInteger.valueOf(EMCValues.get(EMCKey.fromStack(stack)));
    }

    private static void triggerOrbAdvancement(Player player, ItemStack stack) {
        if (EMCOrbItem.isEMCOrb(stack)) {
            ModCriteria.triggerEmcOrb(player, EMCOrbItem.getEmcBig(stack), "created");
        }
    }
}
