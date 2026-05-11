package net.exohayvan.dissolver_enhanced.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class DissolverBlockItem extends BlockItem {
    private static String TOOLTIP_TEXT = "item_tooltip.dissolver_enhanced.dissolver_block_item";
    private static ChatFormatting TOOLTIP_FORMAT = ChatFormatting.GOLD;

    public DissolverBlockItem(Block block, Properties settings) {
		super(block, settings);
	}

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable(TOOLTIP_TEXT).withStyle(TOOLTIP_FORMAT));
    }
    
    @Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
