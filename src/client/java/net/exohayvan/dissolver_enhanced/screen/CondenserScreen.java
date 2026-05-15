package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CondenserScreen extends HandledScreen<CondenserScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/furnace.png");
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;
    private static final int ARROW_TEXTURE_U = 176;
    private static final int ARROW_TEXTURE_V = 14;
    private static final int GUI_BACKGROUND = 0xFFC6C6C6;
    private static final int STATUS_X = 78;
    private static final int RATE_Y = 54;
    private static final int STORED_Y = 64;
    private static final int STATUS_COLOR = 0xFF404040;

    public CondenserScreen(CondenserScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        DrawContextCompat.drawTexture(context, TEXTURE, this.x, this.y, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);

        hideFuelFlame(context);
        drawConversionArrow(context);
    }

    private void hideFuelFlame(DrawContext context) {
        context.fill(
                this.x + FLAME_X,
                this.y + FLAME_Y,
                this.x + FLAME_X + FLAME_WIDTH,
                this.y + FLAME_Y + FLAME_HEIGHT,
                GUI_BACKGROUND
        );
    }

    private void drawConversionArrow(DrawContext context) {
        int progress = this.handler.getScaledProgress();
        int width = Math.min(ARROW_WIDTH, progress);

        if (width > 0) {
            DrawContextCompat.drawTexture(
                    context,
                    TEXTURE,
                    this.x + ARROW_X,
                    this.y + ARROW_Y,
                    ARROW_TEXTURE_U,
                    ARROW_TEXTURE_V,
                    width,
                    ARROW_HEIGHT,
                    256,
                    256
            );
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        DrawContextCompat.drawText(context, this.textRenderer, "Rate: +" + format(this.handler.getCondensingRatePerSecond()) + " EMC/s", STATUS_X, RATE_Y, STATUS_COLOR, false);
        DrawContextCompat.drawText(context, this.textRenderer, "Stored: " + format(this.handler.getStoredEmc()), STATUS_X, STORED_Y, STATUS_COLOR, false);
    }

    private String format(BigInteger value) {
        return EmcNumber.format(value);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
