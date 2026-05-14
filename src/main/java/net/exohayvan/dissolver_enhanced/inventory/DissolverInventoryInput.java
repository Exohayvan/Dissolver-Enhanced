package net.exohayvan.dissolver_enhanced.inventory;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.exohayvan.dissolver_enhanced.advancement.ModCriteria;
import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;

public class DissolverInventoryInput implements Inventory {
    private final DefaultedList<ItemStack> stacks;
    private final int width;
    private final int height;
    private final DissolverScreenHandler handler;
    private PlayerEntity player;

    private int SLOTS = 3;

    public DissolverInventoryInput(DissolverScreenHandler handler, PlayerEntity player) {
        this.stacks = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);
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

    public int size() {
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

    public ItemStack getStack(int slot) {
        return slot >= this.size() ? ItemStack.EMPTY : (ItemStack)this.stacks.get(slot);
    }

    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.stacks, slot);
    }

    public ItemStack removeStack(int slot, int amount) {
        ItemStack itemStack = Inventories.splitStack(this.stacks, slot, amount);
        if (!itemStack.isEmpty()) {
            this.handler.onContentChanged(this);
        }

        return itemStack;
    }

    public void setStack(int slot, ItemStack stack) {
        if (player == null) return;

        boolean NOT_HOLDING_ITEM = stack.getItem() == Items.AIR;
        if (NOT_HOLDING_ITEM) return;

        if (!player.getWorld().isClient()) {
            if (EMCOrbItem.isEMCOrb(stack)) {
                BigInteger emc = EMCOrbItem.getEmcBig(stack);
                if (emc.signum() > 0) {
                    EMCHelper.addEMCValue(player, emc);
                    EMCHelper.sendEmcDeltaToClient(player, emc);
                    ModCriteria.triggerEmcOrb(player, emc, "dissolved");
                    this.stacks.set(slot, ItemStack.EMPTY);
                    this.handler.onContentChanged(this);
                    this.handler.refresh();
                } else {
                    player.getInventory().offerOrDrop(stack);
                }
                return;
            }

            if (slot == 0) {
                if (!EMCHelper.addItem(stack, player, this.handler)) {
                    player.getInventory().offerOrDrop(stack);
                }
                return;
            } else if (slot == 1) {
                String itemId = EMCKey.fromStack(stack);
                if (EMCValues.get(itemId) == 0) {
                    ModAnalytics.captureDissolverItemRejected(namespace(itemId), itemName(itemId), baseItemId(itemId), rejectionReason(itemId));
                    EMCHelper.reportMissingItemValue(player, stack);
                    player.getInventory().offerOrDrop(stack);
                    return;
                }

                EMCHelper.learnItem(player, itemId);
                ModCriteria.triggerLearnedItem(player, itemId);
                this.handler.refresh();
            } else if (slot == 2) {
                String itemId = stack.getItem().toString();
                EMCHelper.forgetItem(player, itemId);
                this.handler.refresh();
            }
        }

        if (slot == 0 && player.getWorld().isClient()) return;

        this.stacks.set(slot, stack);
        this.handler.onContentChanged(this);
    }

    public void markDirty() {
    }

    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    public void clear() {
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

    private static boolean isCreativeItem(String itemId) {
        String baseItemId = baseItemId(itemId);
        return baseItemId.contains("spawn_egg")
            || baseItemId.contains("command_block")
            || baseItemId.contains("bedrock")
            || baseItemId.contains("barrier")
            || baseItemId.contains("structure_block")
            || baseItemId.contains("jigsaw")
            || baseItemId.contains("spawner")
            || baseItemId.contains("vault")
            || baseItemId.contains("end_portal_frame")
            || baseItemId.contains("budding_amethyst")
            || baseItemId.contains("reinforced_deepslate");
    }

    private static String rejectionReason(String itemId) {
        if (isCreativeItem(itemId) && !ModConfig.CREATIVE_ITEMS) {
            return "creative_disabled";
        }

        return "no_emc";
    }
}
