package net.exohayvan.dissolver_enhanced.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DissolverBlockItem extends BlockItem {
    private static String TOOLTIP_TEXT = "item_tooltip.dissolver_enhanced.dissolver_block_item";
    private static Formatting TOOLTIP_FORMAT = Formatting.GOLD;

    public DissolverBlockItem(Block block, Settings settings) {
		super(block, settings);
	}

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(TOOLTIP_TEXT).formatted(TOOLTIP_FORMAT));
    }
    
    @Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
