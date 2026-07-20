package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends MachineScreen<CondenserScreenHandler> {
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackgroundTexture(graphics);
        hideFuelFlame(graphics);
        drawProgressArrow(graphics, menu.getScaledProgress());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(
            font,
            "Rate: +" + formatEmc(menu.getCondensingRatePerSecond()) + " EMC/s",
            STATUS_X,
            RATE_Y,
            STATUS_COLOR,
            false
        );
        graphics.text(
            font,
            "Stored: " + formatEmc(menu.getStoredEmc()),
            STATUS_X,
            STORED_Y,
            STATUS_COLOR,
            false
        );
    }

    private void hideFuelFlame(GuiGraphicsExtractor graphics) {
        graphics.fill(
            leftPos + FLAME_X,
            topPos + FLAME_Y,
            leftPos + FLAME_X + FLAME_WIDTH,
            topPos + FLAME_Y + FLAME_HEIGHT,
            GUI_BACKGROUND
        );
    }
}
