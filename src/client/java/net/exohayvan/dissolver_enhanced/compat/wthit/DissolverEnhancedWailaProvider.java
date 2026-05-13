package net.exohayvan.dissolver_enhanced.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.component.GrowingComponent;
import mcp.mobius.waila.api.component.WrappedComponent;
import mcp.mobius.waila.plugin.harvest.component.ToolComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;

public class DissolverEnhancedWailaProvider implements IBlockComponentProvider {
    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        ItemStack stack = accessor.getStack();
        if (stack.isEmpty()) {
            return;
        }

        String itemId = EMCKey.fromStack(stack);
        var emc = EMCValues.getDisplayBig(itemId);
        if (emc.signum() == 0) {
            return;
        }

        boolean learned = PlayerDataClient.LEARNED_ITEMS.contains(itemId) || PlayerDataClient.LEARNED_ITEMS.contains(EMCKey.baseItemId(itemId));
        tooltip.addLine()
            .with(new WrappedComponent(Text.literal("EMC: " + NumberHelpers.format(emc))))
            .with(GrowingComponent.INSTANCE)
            .with(new ToolComponent(Items.BOOK.getDefaultStack(), learned))
            .with(new WrappedComponent(Text.literal(learned ? " ✓" : " ✕")));
    }
}
