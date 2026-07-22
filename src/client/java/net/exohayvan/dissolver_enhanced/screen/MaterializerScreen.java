package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MaterializerScreen extends AbstractMachineScreen<MaterializerScreenHandler> {
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;

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
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        drawFurnaceBackground(context);
        hideFuelArea(context);
        drawSlotBackground(context, 38, 53);
        drawSlotBackground(context, 56, 53);
        drawProgressArrow(context, this.menu.getScaledProgress());
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


    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        super.renderLabels(context, mouseX, mouseY);
        context.drawString(this.font, "Stored: " + formatEmc(this.menu.getStoredEmc()) + " / " + formatEmc(this.menu.getTargetValue()), STATUS_X, STORED_Y, STATUS_COLOR, false);
        context.drawString(this.font, "Rate: +" + formatEmc(this.menu.getMaterializingRatePerSecond()) + " EMC/s", STATUS_X, RATE_Y, STATUS_COLOR, false);
    }
}
