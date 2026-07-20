package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

abstract class MachineScreen<H extends ScreenHandler> extends HandledScreen<H> {
    protected static final int STATUS_X = 78;
    protected static final int STATUS_COLOR = 0xFF404040;
    protected static final int GUI_BACKGROUND = 0xFFC6C6C6;
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/furnace.png");
    private static final Identifier ARROW_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/burn_progress");
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    protected MachineScreen(H handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
    }

    @Override
    protected final void init() {
        super.init();
        titleX = 8;
        titleY = 6;
        playerInventoryTitleX = 8;
        playerInventoryTitleY = 72;
    }

    @Override
    protected final void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        DrawContextCompat.drawTexture(
            context,
            TEXTURE,
            x,
            y,
            0.0F,
            0.0F,
            backgroundWidth,
            backgroundHeight,
            256,
            256
        );
        drawMachineBackground(context);
        drawProgressArrow(context);
    }

    protected abstract void drawMachineBackground(DrawContext context);

    protected abstract int getScaledProgress();

    private void drawProgressArrow(DrawContext context) {
        int width = Math.min(ARROW_WIDTH, getScaledProgress());
        if (width <= 0) return;

        DrawContextCompat.drawGuiTexture(
            context,
            ARROW_PROGRESS_TEXTURE,
            ARROW_WIDTH,
            ARROW_HEIGHT,
            0,
            0,
            x + ARROW_X,
            y + ARROW_Y,
            width,
            ARROW_HEIGHT
        );
    }

    protected final String format(BigInteger value) {
        return EmcNumber.format(value);
    }

    protected final String format(int value) {
        return format(BigInteger.valueOf(value));
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
