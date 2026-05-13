package net.exohayvan.dissolver_enhanced.item;

import java.util.List;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EmcCoreItem extends Item {
    private final int emcPerSecond;

    public EmcCoreItem(Settings settings, int emcPerSecond) {
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
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(
            "item_tooltip.dissolver_enhanced.emc_core_rate",
            EmcNumber.format(java.math.BigInteger.valueOf(this.emcPerSecond))
        ).formatted(Formatting.AQUA));
    }
}
