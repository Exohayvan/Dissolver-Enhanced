package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DissolverScreen extends AbstractContainerScreen<DissolverScreenHandler> {
    public DissolverScreen(DissolverScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 217, 221);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 31;
        this.inventoryLabelY = this.imageHeight - 93;
    }
}
