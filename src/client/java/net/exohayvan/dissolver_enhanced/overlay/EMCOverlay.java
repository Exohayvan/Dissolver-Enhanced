package net.exohayvan.dissolver_enhanced.overlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.DrawContextCompat;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;

public class EMCOverlay implements HudRenderCallback {
    private static final int WHITE_COLOR = 0xFFFFFF;

    public static void init() {
        HudRenderCallback.EVENT.register(new EMCOverlay());
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null) return;
        if (client.inGameHud.getDebugHud().shouldShowDebugHud() || client.inGameHud.getSpectatorHud().isOpen() || client.options.hudHidden) return;

        if (ModConfig.EMC_ON_HUD == false) return;

        String emc = NumberHelpers.format(PlayerDataClient.EMC);
        Text text = Text.translatable("emc.title", emc);
        DrawContextCompat.drawText(drawContext, client.textRenderer, text, 4, 4, WHITE_COLOR, false);
    }
}
