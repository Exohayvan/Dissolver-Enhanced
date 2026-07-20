package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MaterializerScreen extends MachineScreen<MaterializerScreenHandler> {
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;
    private static final int FLAME_HEIGHT = 14;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int STORED_Y = 54;
    private static final int INPUT_Y = 64;

    public MaterializerScreen(MaterializerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void drawMachineBackground(DrawContext context) {
        context.fill(
            x + FLAME_X - 20,
            y + FLAME_Y,
            x + FLAME_X + FLAME_WIDTH + 5,
            y + FLAME_Y + FLAME_HEIGHT + 18,
            GUI_BACKGROUND
        );
        drawSlotBackground(context, 38, 53);
        drawSlotBackground(context, 56, 53);
    }

    private void drawSlotBackground(DrawContext context, int slotX, int slotY) {
        int left = x + slotX - 1;
        int top = y + slotY - 1;
        context.fill(left, top, left + 18, top + 18, SLOT_DARK);
        context.fill(left + 1, top + 1, left + 18, top + 18, SLOT_LIGHT);
        context.fill(left + 1, top + 1, left + 17, top + 17, SLOT_FILL);
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
            "Stored: " + format(handler.getStoredEmc()) + " / " + format(handler.getTargetValue()),
            STATUS_X,
            STORED_Y,
            STATUS_COLOR,
            false
        );
        DrawContextCompat.drawText(
            context,
            textRenderer,
            "Rate: +" + format(handler.getMaterializingRatePerSecond()) + " EMC/s",
            STATUS_X,
            INPUT_Y,
            STATUS_COLOR,
            false
        );
    }
}
