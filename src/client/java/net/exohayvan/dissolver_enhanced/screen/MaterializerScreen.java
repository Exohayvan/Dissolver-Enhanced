package net.exohayvan.dissolver_enhanced.screen;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MaterializerScreen extends HandledScreen<MaterializerScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/furnace.png");
    private static final Identifier ARROW_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/burn_progress");
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
    private static final int INPUT_Y = 64;
    private static final int STATUS_COLOR = 0xFF404040;

    public MaterializerScreen(MaterializerScreenHandler handler, PlayerInventory inventory, Text title) {
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

        hideFuelArea(context);
        drawSlotBackground(context, 38, 53);
        drawSlotBackground(context, 56, 53);
        drawProgressArrow(context);
    }

    private void hideFuelArea(DrawContext context) {
        context.fill(
                this.x + FLAME_X - 20,
                this.y + FLAME_Y,
                this.x + FLAME_X + FLAME_WIDTH + 5,
                this.y + FLAME_Y + FLAME_HEIGHT + 18,
                GUI_BACKGROUND
        );
    }

    private void drawSlotBackground(DrawContext context, int slotX, int slotY) {
        int left = this.x + slotX - 1;
        int top = this.y + slotY - 1;
        context.fill(left, top, left + 18, top + 18, SLOT_DARK);
        context.fill(left + 1, top + 1, left + 18, top + 18, SLOT_LIGHT);
        context.fill(left + 1, top + 1, left + 17, top + 17, SLOT_FILL);
    }

    private void drawProgressArrow(DrawContext context) {
        int progress = this.handler.getScaledProgress();
        int width = Math.min(ARROW_WIDTH, progress);

        if (width > 0) {
            DrawContextCompat.drawGuiTexture(
                    context,
                    ARROW_PROGRESS_TEXTURE,
                    ARROW_WIDTH,
                    ARROW_HEIGHT,
                    0,
                    0,
                    this.x + ARROW_X,
                    this.y + ARROW_Y,
                    width,
                    ARROW_HEIGHT
            );
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        context.drawText(this.textRenderer, "Stored: " + format(this.handler.getStoredEmc()) + " / " + format(this.handler.getTargetValue()), STATUS_X, STORED_Y, STATUS_COLOR, false);
        context.drawText(this.textRenderer, "Rate: +" + format(this.handler.getMaterializingRatePerSecond()) + " EMC/s", STATUS_X, INPUT_Y, STATUS_COLOR, false);
    }

    private String format(int value) {
        return EmcNumber.format(BigInteger.valueOf(value));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
