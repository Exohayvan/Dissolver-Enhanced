package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class CondenserScreen extends MachineScreen<CondenserScreenHandler> {
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int RATE_Y = 54;
    private static final int STORED_Y = 64;

    public CondenserScreen(CondenserScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void drawMachineBackground(DrawContext context) {
        context.fill(
            x + FLAME_X,
            y + FLAME_Y,
            x + FLAME_X + FLAME_WIDTH,
            y + FLAME_Y + FLAME_HEIGHT,
            GUI_BACKGROUND
        );
    }

    @Override
    protected int getScaledProgress() {
        return handler.getScaledProgress();
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        DrawContextCompat.drawText(
            context,
            textRenderer,
            "Rate: +" + format(handler.getCondensingRatePerSecond()) + " EMC/s",
            STATUS_X,
            RATE_Y,
            STATUS_COLOR,
            false
        );
        DrawContextCompat.drawText(
            context,
            textRenderer,
            "Stored: " + format(handler.getStoredEmc()),
            STATUS_X,
            STORED_Y,
            STATUS_COLOR,
            false
        );
    }
}
