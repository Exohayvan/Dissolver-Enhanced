package net.exohayvan.dissolver_enhanced.item;

import java.math.BigInteger;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
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
        ItemStack stack = new ItemStack(ModItems.EMC_ORB);
        setEMC(stack, emc);
        return stack;
    }

    public static int getEMC(ItemStack stack) {
        return EmcNumber.toIntSaturated(getEmcBig(stack));
    }

    public static BigInteger getEmcBig(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        if (nbt.contains(EMC_KEY) && nbt.getString(EMC_KEY).isPresent()) {
            return EmcNumber.parse(nbt.getString(EMC_KEY).orElse("0"));
        }

        return EmcNumber.of(nbt.getInt(EMC_KEY).orElse(0));
    }

    public static void setEMC(ItemStack stack, int emc) {
        setEMC(stack, BigInteger.valueOf(emc));
    }

    public static void setEMC(ItemStack stack, BigInteger emc) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putString(EMC_KEY, EmcNumber.nonNegative(emc).toString()));
    }

    public static boolean isEMCOrb(ItemStack stack) {
        return stack.is(ModItems.EMC_ORB);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable("item_tooltip.dissolver_enhanced.emc_orb", EmcNumber.format(getEmcBig(stack))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEmcBig(stack).signum() > 0;
    }
}
