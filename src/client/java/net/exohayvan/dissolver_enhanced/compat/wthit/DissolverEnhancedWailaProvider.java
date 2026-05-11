package net.exohayvan.dissolver_enhanced.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.component.GrowingComponent;
import mcp.mobius.waila.api.component.WrappedComponent;
import mcp.mobius.waila.plugin.harvest.component.ToolComponent;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DissolverEnhancedWailaProvider implements IBlockComponentProvider {
    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        ItemStack stack = accessor.getStack();
        if (stack.isEmpty()) {
            return;
        }

        String itemId = EMCKey.fromStack(stack);
        int emc = EMCValues.getDisplay(itemId);
        if (emc == 0) {
            return;
        }

        boolean learned = PlayerDataClient.LEARNED_ITEMS.contains(itemId) || PlayerDataClient.LEARNED_ITEMS.contains(EMCKey.baseItemId(itemId));
        tooltip.addLine()
            .with(new WrappedComponent(Component.literal("EMC: " + NumberHelpers.format(emc))))
            .with(GrowingComponent.INSTANCE)
            .with(new ToolComponent(Items.BOOK.getDefaultInstance(), learned))
            .with(new WrappedComponent(Component.literal(learned ? " ✓" : " ✕")));
    }
}
