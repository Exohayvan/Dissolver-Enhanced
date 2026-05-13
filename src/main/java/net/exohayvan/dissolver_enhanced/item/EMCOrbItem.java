package net.exohayvan.dissolver_enhanced.item;

import java.math.BigInteger;
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
    private static final int NBT_STRING_TYPE = 8;

    public EMCOrbItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(int emc) {
        return create(BigInteger.valueOf(emc));
    }

    public static ItemStack create(BigInteger emc) {
        ItemStack stack = new ItemStack(ModItems.EMC_ORB);
        setEMC(stack, emc);
        return stack;
    }

    public static int getEMC(ItemStack stack) {
        return EmcNumber.toIntSaturated(getEmcBig(stack));
    }

    public static BigInteger getEmcBig(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        var nbt = customData.copyNbt();
        if (nbt.contains(EMC_KEY, NBT_STRING_TYPE)) {
            return EmcNumber.parse(nbt.getString(EMC_KEY));
        }

        return EmcNumber.of(nbt.getInt(EMC_KEY));
    }

    public static void setEMC(ItemStack stack, int emc) {
        setEMC(stack, BigInteger.valueOf(emc));
    }

    public static void setEMC(ItemStack stack, BigInteger emc) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(EMC_KEY, EmcNumber.nonNegative(emc).toString()));
    }

    public static boolean isEMCOrb(ItemStack stack) {
        return stack.isOf(ModItems.EMC_ORB);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item_tooltip.dissolver_enhanced.emc_orb", EmcNumber.format(getEmcBig(stack))).formatted(Formatting.LIGHT_PURPLE));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return getEmcBig(stack).signum() > 0;
    }
}
