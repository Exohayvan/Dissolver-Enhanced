package net.exohayvan.dissolver_enhanced.item;


import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class EmcCoreItem extends Item {
    private final int emcPerSecond;

    public EmcCoreItem(Properties settings, int emcPerSecond) {
        super(settings);
        this.emcPerSecond = Math.max(1, emcPerSecond);
    }

    public int getEmcPerSecond() {
        return this.emcPerSecond;
    }

    public static boolean isEmcCore(ItemStack stack) {
        return stack.getItem() instanceof EmcCoreItem;
    }

    public static int getEmcPerSecond(ItemStack stack) {
        if (stack.getItem() instanceof EmcCoreItem emcCoreItem) {
            return emcCoreItem.getEmcPerSecond();
        }

        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable(
            "item_tooltip.dissolver_enhanced.emc_core_rate",
            EmcNumber.format(java.math.BigInteger.valueOf(this.emcPerSecond))
        ).withStyle(ChatFormatting.AQUA));
    }
}
