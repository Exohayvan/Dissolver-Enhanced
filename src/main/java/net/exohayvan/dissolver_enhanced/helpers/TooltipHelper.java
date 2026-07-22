package net.exohayvan.dissolver_enhanced.helpers;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static int insertIndexAfterModName(ItemStack stack, List<Component> tooltip) {
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