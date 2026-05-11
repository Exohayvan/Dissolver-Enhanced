package net.exohayvan.dissolver_enhanced.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraftforge.fml.ModList;

@Mixin(value = ItemStack.class, priority = 500)
public class ItemStackTooltipMixin {
    @Inject(method = "getTooltip", at = @At(value = "RETURN", ordinal = 1))
    private void addEmcTooltip(@Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        String itemId = EMCKey.fromStack(stack);
        Component formattedText = EMCHelper.tooltipValue(itemId, ItemHelper.getDurabilityPercentage(stack));
        if (formattedText.getString().isEmpty()) return;

        List<Component> tooltip = cir.getReturnValue();
        tooltip.add(getInsertIndexAfterModName(stack, tooltip), formattedText);
    }

    private int getInsertIndexAfterModName(ItemStack stack, List<Component> tooltip) {
        ResourceLocation id = ResourceLocation.tryParse(EMCKey.baseItemId(EMCKey.fromStack(stack)));
        if (id == null) return tooltip.size();

        String modName = ModList.get()
            .getModContainerById(id.getNamespace())
            .map(container -> container.getModInfo().getDisplayName())
            .orElse(null);
        if (modName == null) return tooltip.size();

        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (tooltip.get(i).getString().equals(modName)) {
                return i + 1;
            }
        }

        return tooltip.size();
    }
}
