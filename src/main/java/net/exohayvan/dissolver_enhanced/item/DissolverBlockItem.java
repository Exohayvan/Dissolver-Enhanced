package net.exohayvan.dissolver_enhanced.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class DissolverBlockItem extends BlockItem {
    private static final String TOOLTIP_TEXT = "item_tooltip.dissolver_enhanced.dissolver_block_item";

    public DissolverBlockItem(Block block, Properties settings) {
		super(block, settings);
	}

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        ItemPresentation.appendGoldenTooltip(tooltip, TOOLTIP_TEXT);
    }
    
    @Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
