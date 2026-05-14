package net.exohayvan.dissolver_enhanced.event;

import java.util.List;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
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
        tooltip.add(getInsertIndexAfterModName(stack, tooltip), formattedText);
    }

    private static int getInsertIndexAfterModName(ItemStack stack, List<Component> tooltip) {
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
