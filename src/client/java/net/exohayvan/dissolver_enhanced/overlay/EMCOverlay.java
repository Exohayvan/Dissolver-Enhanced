package net.exohayvan.dissolver_enhanced.overlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;

public class EMCOverlay implements HudRenderCallback {
    private static final int WHITE_COLOR = 0xFFFFFF;

    public static void init() {
        HudRenderCallback.EVENT.register(new EMCOverlay());
    }

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client == null) return;
        if (client.gui.getDebugOverlay().showDebugScreen() || client.gui.getSpectatorGui().isMenuActive() || client.options.hideGui) return;

        if (ModConfig.EMC_ON_HUD == false) return;

        String emc = NumberHelpers.format(PlayerDataClient.EMC);
        Component text = Component.translatable("emc.title", emc);
        drawContext.drawString(client.font, text, 4, 4, WHITE_COLOR, false);
    }
}
