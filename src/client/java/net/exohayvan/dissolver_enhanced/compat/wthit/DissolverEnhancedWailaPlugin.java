package net.exohayvan.dissolver_enhanced.compat.wthit;

import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.block.Block;

public class DissolverEnhancedWailaPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new DissolverEnhancedWailaProvider(), Block.class);
    }
}
