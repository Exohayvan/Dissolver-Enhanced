package net.exohayvan.dissolver_enhanced.item;

import java.math.BigInteger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EMCOrbItem extends Item {
    private static final String EMC_KEY = "dissolver_enhanced.emc";
    private static final int NBT_STRING_TYPE = 8;

    public EMCOrbItem(Properties settings) {
        super(settings);
    }

    public static ItemStack create(int emc) {
        return create(BigInteger.valueOf(emc));
    }

    public static ItemStack create(BigInteger emc) {
        ItemStack stack = new ItemStack(ModItems.EMC_ORB.get());
        setEMC(stack, emc);
        return stack;
    }

    public static int getEMC(ItemStack stack) {
        return EmcNumber.toIntSaturated(getEmcBig(stack));
    }

    public static BigInteger getEmcBig(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.contains(EMC_KEY, NBT_STRING_TYPE)) {
            return EmcNumber.parse(nbt.getString(EMC_KEY));
        }

        return EmcNumber.of(nbt.getInt(EMC_KEY));
    }

    public static void setEMC(ItemStack stack, int emc) {
        setEMC(stack, BigInteger.valueOf(emc));
    }

    public static void setEMC(ItemStack stack, BigInteger emc) {
        stack.getOrCreateTag().putString(EMC_KEY, EmcNumber.nonNegative(emc).toString());
    }

    public static boolean isEMCOrb(ItemStack stack) {
        return stack.is(ModItems.EMC_ORB.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, java.util.List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable("item_tooltip.dissolver_enhanced.emc_orb", EmcNumber.format(getEmcBig(stack))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEmcBig(stack).signum() > 0;
    }
}
