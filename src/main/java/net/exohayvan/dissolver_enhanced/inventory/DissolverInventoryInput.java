package net.exohayvan.dissolver_enhanced.inventory;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DissolverInventoryInput extends DissolverInventory {
    private final DissolverScreenHandler handler;
    private Player player;

    private int SLOTS = 3;

    public DissolverInventoryInput(DissolverScreenHandler handler, Player player) {
        super(handler, 3, 1);
        this.handler = handler;
        this.player = player;
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

    @Override
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
                    ModAnalytics.captureDissolverItemRejected(namespace(itemId), itemName(itemId), baseItemId(itemId), rejectionReason(itemId));
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


    private static String baseItemId(String itemId) {
        return EMCKey.baseItemId(itemId);
    }

    private static String namespace(String itemId) {
        String baseItemId = baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? "unknown" : baseItemId.substring(0, namespaceEnd);
    }

    private static String itemName(String itemId) {
        String baseItemId = baseItemId(itemId);
        int namespaceEnd = baseItemId.indexOf(":");
        return namespaceEnd == -1 ? baseItemId : baseItemId.substring(namespaceEnd + 1);
    }

    private static String rejectionReason(String itemId) {
        return EMCHelper.rejectionReason(itemId);
    }
}
