package net.vassbo.vanillaemc.compat.wthit;

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
import net.vassbo.vanillaemc.data.EMCValues;
import net.vassbo.vanillaemc.data.PlayerDataClient;
import net.vassbo.vanillaemc.helpers.EMCKey;
import net.vassbo.vanillaemc.helpers.NumberHelpers;

public class VanillaEMCWailaProvider implements IBlockComponentProvider {
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
            .with(new WrappedComponent(Text.literal("EMC: " + NumberHelpers.format(emc))))
            .with(GrowingComponent.INSTANCE)
            .with(new ToolComponent(Items.BOOK.getDefaultStack(), learned))
            .with(new WrappedComponent(Text.literal(learned ? " ✓" : " ✕")));
    }
}
