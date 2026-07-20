package net.exohayvan.dissolver_enhanced.inventory;

import static net.exohayvan.dissolver_enhanced.helpers.EmcItemClassifier.baseItemId;
import static net.exohayvan.dissolver_enhanced.helpers.EmcItemClassifier.itemName;
import static net.exohayvan.dissolver_enhanced.helpers.EmcItemClassifier.namespace;
import static net.exohayvan.dissolver_enhanced.helpers.EmcItemClassifier.rejectionReason;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.WorldCompat;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

public class DissolverInventoryInput extends BaseDissolverInventory<DissolverScreenHandler> {
    private static final int SLOTS = 3;
    private final PlayerEntity player;

    public DissolverInventoryInput(DissolverScreenHandler handler, PlayerEntity player) {
        super(handler, SLOTS, 1, DefaultedList.ofSize(SLOTS, ItemStack.EMPTY));
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
        return SLOTS;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (player == null || stack.getItem() == Items.AIR) return;

        if (!WorldCompat.isClient(player)) {
            if (EMCOrbItem.isEMCOrb(stack)) {
                dissolveOrb(slot, stack);
                return;
            }

            if (slot == 0) {
                if (!EMCHelper.addItem(stack, player, handler)) {
                    player.getInventory().offerOrDrop(stack);
                }
                return;
            }
            if (slot == 1) {
                if (!learnItem(stack)) return;
            } else if (slot == 2) {
                EMCHelper.forgetItem(player, stack.getItem().toString());
                handler.refresh();
            }
        }

        if (slot == 0 && WorldCompat.isClient(player)) return;
        super.setStack(slot, stack);
    }

    private void dissolveOrb(int slot, ItemStack stack) {
        BigInteger emc = EMCOrbItem.getEmcBig(stack);
        if (emc.signum() <= 0) {
            player.getInventory().offerOrDrop(stack);
            return;
        }

        EMCHelper.addEMCValue(player, emc);
        EMCHelper.sendEmcDeltaToClient(player, emc);
        ModCriteria.triggerEmcOrb(player, emc, "dissolved");
        stacks.set(slot, ItemStack.EMPTY);
        handler.onContentChanged(this);
        handler.refresh();
    }

    private boolean learnItem(ItemStack stack) {
        String itemId = EMCKey.fromStack(stack);
        if (EMCValues.get(itemId) == 0) {
            ModAnalytics.captureDissolverItemRejected(
                namespace(itemId),
                itemName(itemId),
                baseItemId(itemId),
                rejectionReason(itemId)
            );
            EMCHelper.reportMissingItemValue(player, stack);
            player.getInventory().offerOrDrop(stack);
            return false;
        }

        EMCHelper.learnItem(player, itemId);
        ModCriteria.triggerLearnedItem(player, itemId);
        handler.refresh();
        return true;
    }
}
