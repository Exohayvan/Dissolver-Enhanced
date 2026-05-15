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

public class MaterializerScreen extends AbstractContainerScreen<MaterializerScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;
    private static final int GUI_BACKGROUND = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int STATUS_X = 78;
    private static final int STORED_Y = 54;
    private static final int RATE_Y = 64;
    private static final int STATUS_COLOR = 0xFF404040;

    public MaterializerScreen(MaterializerScreenHandler handler, Inventory inventory, Component title) {
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

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        context.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        hideFuelArea(context);
        drawSlotBackground(context, 38, 53);
        drawSlotBackground(context, 56, 53);
        drawProgressArrow(context);
    }

    private void hideFuelArea(GuiGraphics context) {
        context.fill(this.leftPos + FLAME_X - 20, this.topPos + FLAME_Y, this.leftPos + FLAME_X + FLAME_WIDTH + 5, this.topPos + FLAME_Y + FLAME_HEIGHT + 18, GUI_BACKGROUND);
    }

    private void drawSlotBackground(GuiGraphics context, int slotX, int slotY) {
        int left = this.leftPos + slotX - 1;
        int top = this.topPos + slotY - 1;
        context.fill(left, top, left + 18, top + 18, SLOT_DARK);
        context.fill(left + 1, top + 1, left + 18, top + 18, SLOT_LIGHT);
        context.fill(left + 1, top + 1, left + 17, top + 17, SLOT_FILL);
    }

    private void drawProgressArrow(GuiGraphics context) {
        int width = Math.min(ARROW_WIDTH, this.menu.getScaledProgress());
        if (width <= 0) return;
        context.blitSprite(ARROW_TEXTURE, ARROW_WIDTH, ARROW_HEIGHT, 0, 0, this.leftPos + ARROW_X, this.topPos + ARROW_Y, width, ARROW_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        super.renderLabels(context, mouseX, mouseY);
        context.drawString(this.font, "Stored: " + format(this.menu.getStoredEmc()) + " / " + format(this.menu.getTargetValue()), STATUS_X, STORED_Y, STATUS_COLOR, false);
        context.drawString(this.font, "Rate: +" + format(this.menu.getMaterializingRatePerSecond()) + " EMC/s", STATUS_X, RATE_Y, STATUS_COLOR, false);
    }

    private String format(int value) {
        return EmcNumber.format(BigInteger.valueOf(value));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
