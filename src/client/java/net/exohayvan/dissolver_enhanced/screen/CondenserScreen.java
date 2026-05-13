package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends AbstractContainerScreen<CondenserScreenHandler> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier ARROW_PROGRESS_TEXTURE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;
    private static final int GUI_BACKGROUND = 0xFFC6C6C6;
    private static final int STATUS_X = 78;
    private static final int RATE_Y = 54;
    private static final int STORED_Y = 64;
    private static final int STATUS_COLOR = 0xFF404040;

    public CondenserScreen(CondenserScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        hideFuelFlame(graphics);
        drawConversionArrow(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(this.font, "Rate: +" + format(this.menu.getCondensingRatePerSecond()) + " EMC/s", STATUS_X, RATE_Y, STATUS_COLOR, false);
        graphics.text(this.font, "Stored: " + format(this.menu.getStoredEmc()), STATUS_X, STORED_Y, STATUS_COLOR, false);
    }

    private void hideFuelFlame(GuiGraphicsExtractor graphics) {
        graphics.fill(
            this.leftPos + FLAME_X,
            this.topPos + FLAME_Y,
            this.leftPos + FLAME_X + FLAME_WIDTH,
            this.topPos + FLAME_Y + FLAME_HEIGHT,
            GUI_BACKGROUND
        );
    }

    private void drawConversionArrow(GuiGraphicsExtractor graphics) {
        int width = Math.min(ARROW_WIDTH, this.menu.getScaledProgress());
        if (width <= 0) return;

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            ARROW_PROGRESS_TEXTURE,
            ARROW_WIDTH,
            ARROW_HEIGHT,
            0,
            0,
            this.leftPos + ARROW_X,
            this.topPos + ARROW_Y,
            width,
            ARROW_HEIGHT
        );
    }

    private String format(BigInteger value) {
        return EmcNumber.format(value);
    }
}
