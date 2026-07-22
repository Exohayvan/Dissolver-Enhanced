package net.exohayvan.dissolver_enhanced.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.helpers.TooltipHelper;

@Mixin(value = ItemStack.class, priority = 500)
public class ItemStackTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void addEmcTooltip(@Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        String itemId = EMCKey.fromStack(stack);
        Component formattedText = EMCHelper.tooltipValue(itemId, ItemHelper.getDurabilityPercentage(stack));
        if (formattedText.getString().isEmpty()) return;

        List<Component> tooltip = cir.getReturnValue();
        tooltip.add(TooltipHelper.insertIndexAfterModName(stack, tooltip), formattedText);
    }
}
