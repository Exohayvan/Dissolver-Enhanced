package net.exohayvan.dissolver_enhanced.item;

import java.util.List;
import net.exohayvan.dissolver_enhanced.helpers.WirelessDissolver;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CrystalFrameItem extends Item {
    private static String TOOLTIP_TEXT = "item_tooltip.dissolver_enhanced.crystal_frame_item";
    private static ChatFormatting TOOLTIP_FORMAT = ChatFormatting.GOLD;

    public CrystalFrameItem(Properties settings) {
		super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable(TOOLTIP_TEXT).withStyle(TOOLTIP_FORMAT));
    }
    
    @Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
    
    @Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (!world.isClientSide() && !WirelessDissolver.open(user, world)) {
            user.displayClientMessage(Component.translatable("wireless_open.fail", WirelessDissolver.radius), true);
        }

        return InteractionResultHolder.pass(user.getItemInHand(hand));
	}
}
