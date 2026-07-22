package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends AbstractMachineScreen<CondenserScreenHandler> {
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;

    private static final int GUI_BACKGROUND = 0xFFC6C6C6;
    private static final int STATUS_X = 78;
    private static final int RATE_Y = 54;
    private static final int STORED_Y = 64;
    private static final int STATUS_COLOR = 0xFF404040;

    public CondenserScreen(CondenserScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        drawFurnaceBackground(context);
        hideFuelFlame(context);
        drawProgressArrow(context, this.menu.getScaledProgress());
    }

    private void hideFuelFlame(GuiGraphics context) {
        context.fill(this.leftPos + FLAME_X, this.topPos + FLAME_Y, this.leftPos + FLAME_X + FLAME_WIDTH, this.topPos + FLAME_Y + FLAME_HEIGHT, GUI_BACKGROUND);
    }


    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        super.renderLabels(context, mouseX, mouseY);
        context.drawString(this.font, "Rate: +" + formatEmc(this.menu.getCondensingRatePerSecond()) + " EMC/s", STATUS_X, RATE_Y, STATUS_COLOR, false);
        context.drawString(this.font, "Stored: " + formatEmc(this.menu.getStoredEmc()), STATUS_X, STORED_Y, STATUS_COLOR, false);
    }
}
