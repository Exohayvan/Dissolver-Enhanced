package net.exohayvan.dissolver_enhanced.event;

import java.util.List;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.helpers.TooltipHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DissolverEnhanced.MOD_ID, value = Dist.CLIENT)
public class ForgeItemTooltipEvent {
    private ForgeItemTooltipEvent() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String itemId = EMCKey.fromStack(stack);
        Component formattedText = EMCHelper.tooltipValue(itemId, ItemHelper.getDurabilityPercentage(stack));
        if (formattedText.getString().isEmpty()) return;

        List<Component> tooltip = event.getToolTip();
        tooltip.add(TooltipHelper.insertIndexAfterModName(stack, tooltip), formattedText);
    }
}
