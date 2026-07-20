package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier ARROW_PROGRESS_TEXTURE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    protected MachineScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    protected final void drawBackgroundTexture(GuiGraphicsExtractor graphics) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            leftPos,
            topPos,
            0,
            0,
            imageWidth,
            imageHeight,
            256,
            256
        );
    }

    protected final void drawProgressArrow(GuiGraphicsExtractor graphics, int scaledProgress) {
        int width = Math.min(ARROW_WIDTH, scaledProgress);
        if (width <= 0) return;

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            ARROW_PROGRESS_TEXTURE,
            ARROW_WIDTH,
            ARROW_HEIGHT,
            0,
            0,
            leftPos + ARROW_X,
            topPos + ARROW_Y,
            width,
            ARROW_HEIGHT
        );
    }

    protected final String formatEmc(BigInteger value) {
        return EmcNumber.format(value);
    }

    protected final String formatEmc(int value) {
        return formatEmc(BigInteger.valueOf(value));
    }
}
