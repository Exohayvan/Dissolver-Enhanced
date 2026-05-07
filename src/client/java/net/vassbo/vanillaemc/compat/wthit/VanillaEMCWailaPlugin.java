package net.vassbo.vanillaemc.compat.wthit;

import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.block.Block;

public class VanillaEMCWailaPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new VanillaEMCWailaProvider(), Block.class);
    }
}
