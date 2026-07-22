package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

abstract class AbstractMachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected static final ResourceLocation FURNACE_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    protected static final int ARROW_X = 79;
    protected static final int ARROW_Y = 34;
    protected static final int ARROW_WIDTH = 24;
    protected static final int ARROW_HEIGHT = 16;

    protected AbstractMachineScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    protected void drawProgressArrow(GuiGraphics context, int progress) {
        int width = Math.min(ARROW_WIDTH, progress);
        if (width > 0) {
            context.blit(FURNACE_TEXTURE, this.leftPos + ARROW_X, this.topPos + ARROW_Y, 176, 14, width, ARROW_HEIGHT);
        }
    }

    protected void drawFurnaceBackground(GuiGraphics context) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, FURNACE_TEXTURE);
        context.blit(FURNACE_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected static String formatEmc(BigInteger value) {
        return EmcNumber.format(value);
    }

    protected static String formatEmc(int value) {
        return formatEmc(BigInteger.valueOf(value));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}