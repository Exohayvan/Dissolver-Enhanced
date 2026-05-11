package net.exohayvan.dissolver_enhanced.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(at = @At("RETURN"), method = "appendHoverText", cancellable = true)
    public void appendTooltip(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag type, CallbackInfo ci) {
        String itemId = EMCKey.fromStack(stack);

        // add emc value
        Component formattedText = EMCHelper.tooltipValue(itemId, ItemHelper.getDurabilityPercentage(stack));
        if (!formattedText.getString().isEmpty()) tooltip.add(formattedText);
    }
}
