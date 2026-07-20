package net.exohayvan.dissolver_enhanced.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MaterializerScreen extends MachineScreen<MaterializerScreenHandler> {
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackgroundTexture(graphics);
        hideFuelArea(graphics);
        drawSlotBackground(graphics, 38, 53);
        drawSlotBackground(graphics, 56, 53);
        drawProgressArrow(graphics, menu.getScaledProgress());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(
            font,
            "Stored: " + formatEmc(menu.getStoredEmc()) + " / " + formatEmc(menu.getTargetValue()),
            STATUS_X,
            STORED_Y,
            STATUS_COLOR,
            false
        );
        graphics.text(
            font,
            "Rate: +" + formatEmc(menu.getMaterializingRatePerSecond()) + " EMC/s",
            STATUS_X,
            RATE_Y,
            STATUS_COLOR,
            false
        );
    }

    private void hideFuelArea(GuiGraphicsExtractor graphics) {
        graphics.fill(
            leftPos + FLAME_X - 20,
            topPos + FLAME_Y,
            leftPos + FLAME_X + FLAME_WIDTH + 5,
            topPos + FLAME_Y + FLAME_HEIGHT + 18,
            GUI_BACKGROUND
        );
    }

    private void drawSlotBackground(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        int left = leftPos + slotX - 1;
        int top = topPos + slotY - 1;
        graphics.fill(left, top, left + 18, top + 18, SLOT_DARK);
        graphics.fill(left + 1, top + 1, left + 18, top + 18, SLOT_LIGHT);
        graphics.fill(left + 1, top + 1, left + 17, top + 17, SLOT_FILL);
    }
}
