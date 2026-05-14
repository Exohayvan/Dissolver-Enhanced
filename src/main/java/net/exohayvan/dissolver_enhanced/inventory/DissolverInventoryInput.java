package net.exohayvan.dissolver_enhanced.inventory;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DissolverInventoryInput implements Container {
    private final NonNullList<ItemStack> stacks;
    private final int width;
    private final int height;
    private final DissolverScreenHandler handler;
    private Player player;

    private int SLOTS = 3;

    public DissolverInventoryInput(DissolverScreenHandler handler, Player player) {
        this.stacks = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        this.handler = handler;
        this.player = player;
        this.width = SLOTS;
        this.height = 1;
    }

    public DissolverSlotInput getInputSlot() {
        return new DissolverSlotInput(this, 0, 7, 18);
    }

    public Slot getAdderSlot() {
        return new Slot(this, 1, 7, 54);
    }

    public Slot getRemoverSlot() {
        return new Slot(this, 2, 7, 72);
    }

    public int slots() {
        return this.SLOTS;
    }

    public int getContainerSize() {
        return this.stacks.size();
    }

    public boolean isEmpty() {
        Iterator<ItemStack> var1 = this.stacks.iterator();

        ItemStack itemStack;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            itemStack = (ItemStack)var1.next();
        } while(itemStack.isEmpty());

        return false;
    }

    public ItemStack getItem(int slot) {
        return slot >= this.getContainerSize() ? ItemStack.EMPTY : (ItemStack)this.stacks.get(slot);
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.stacks, slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = ContainerHelper.removeItem(this.stacks, slot, amount);
        if (!itemStack.isEmpty()) {
            this.handler.slotsChanged(this);
        }

        return itemStack;
    }

    public void setItem(int slot, ItemStack stack) {
        if (player == null) return;

        boolean NOT_HOLDING_ITEM = stack.getItem() == Items.AIR;
        if (NOT_HOLDING_ITEM) return;

        if (!player.level().isClientSide()) {
            if (EMCOrbItem.isEMCOrb(stack)) {
                BigInteger emc = EMCOrbItem.getEmcBig(stack);
                if (emc.signum() > 0) {
                    EMCHelper.addEMCValue(player, emc);
                    EMCHelper.sendEmcDeltaToClient(player, emc);
                    ModCriteria.triggerEmcOrb(player, emc, "dissolved");
                    this.stacks.set(slot, ItemStack.EMPTY);
                    this.handler.slotsChanged(this);
                    this.handler.refresh();
                } else {
                    player.getInventory().placeItemBackInInventory(stack);
                }
                return;
            }

            if (slot == 0) {
                if (!EMCHelper.addItem(stack, player, this.handler)) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
                return;
            } else if (slot == 1) {
                String itemId = EMCKey.fromStack(stack);
                if (EMCValues.get(itemId) == 0) {
                    EMCHelper.reportMissingItemValue(player, stack);
                    player.getInventory().placeItemBackInInventory(stack);
                    return;
                }

                EMCHelper.learnItem(player, itemId);
                ModCriteria.triggerLearnedItem(player, itemId);
                player.getInventory().placeItemBackInInventory(stack);
                this.handler.refresh();
                return;
            } else if (slot == 2) {
                String itemId = EMCKey.fromStack(stack);
                EMCHelper.forgetItem(player, itemId);
                player.getInventory().placeItemBackInInventory(stack);
                this.handler.refresh();
                return;
            }
        }

        if (slot == 0 && player.level().isClientSide()) return;

        this.stacks.set(slot, stack);
        this.handler.slotsChanged(this);
    }

    public void setChanged() {
    }

    public boolean stillValid(Player player) {
        return true;
    }

    public void clearContent() {
        this.stacks.clear();
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public List<ItemStack> getHeldStacks() {
        return List.copyOf(this.stacks);
    }
}
