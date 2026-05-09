package net.vassbo.vanillaemc.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.vassbo.vanillaemc.helpers.EMCHelper;
import net.vassbo.vanillaemc.helpers.EMCKey;
import net.vassbo.vanillaemc.helpers.ItemHelper;

@Mixin(value = ItemStack.class, priority = 500)
public class ItemStackTooltipMixin {
    @Inject(method = "getTooltip", at = @At(value = "RETURN", ordinal = 1))
    private void addEmcTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack)(Object)this;
        String itemId = EMCKey.fromStack(stack);
        Text formattedText = EMCHelper.tooltipValue(itemId, ItemHelper.getDurabilityPercentage(stack));
        if ("".equals(formattedText.getLiteralString())) return;

        List<Text> tooltip = cir.getReturnValue();
        tooltip.add(getInsertIndexAfterModName(stack, tooltip), formattedText);
    }

    private int getInsertIndexAfterModName(ItemStack stack, List<Text> tooltip) {
        Identifier id = Identifier.tryParse(EMCKey.baseItemId(EMCKey.fromStack(stack)));
        if (id == null) return tooltip.size();

        String modName = FabricLoader.getInstance()
            .getModContainer(id.getNamespace())
            .map(container -> container.getMetadata().getName())
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
