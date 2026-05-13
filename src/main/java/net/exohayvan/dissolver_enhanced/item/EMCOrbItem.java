package net.exohayvan.dissolver_enhanced.item;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EMCOrbItem extends Item {
    private static final String EMC_KEY = "dissolver_enhanced.emc";

    public EMCOrbItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(int emc) {
        ItemStack stack = new ItemStack(ModItems.EMC_ORB);
        setEMC(stack, emc);
        return stack;
    }

    public static int getEMC(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return Math.max(0, customData.copyNbt().getInt(EMC_KEY));
    }

    public static void setEMC(ItemStack stack, int emc) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(EMC_KEY, Math.max(0, emc)));
    }

    public static boolean isEMCOrb(ItemStack stack) {
        return stack.isOf(ModItems.EMC_ORB);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item_tooltip.dissolver_enhanced.emc_orb", EmcNumber.format(java.math.BigInteger.valueOf(getEMC(stack)))).formatted(Formatting.LIGHT_PURPLE));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return getEMC(stack) > 0;
    }
}
