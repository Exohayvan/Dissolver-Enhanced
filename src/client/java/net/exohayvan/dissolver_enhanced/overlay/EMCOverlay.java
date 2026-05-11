package net.exohayvan.dissolver_enhanced.overlay;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DissolverEnhanced.MOD_ID)
public class EMCOverlay {
    private static final int WHITE_COLOR = 0xFFFFFF;

    public static void init() {
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        Minecraft client = Minecraft.getInstance();

        if (client == null) return;
        if (client.options.renderDebug || client.gui.getSpectatorGui().isMenuActive() || client.options.hideGui) return;

        if (ModConfig.EMC_ON_HUD == false) return;

        String emc = NumberHelpers.format(PlayerDataClient.EMC);
        Component text = Component.translatable("emc.title", emc);
        GuiGraphics drawContext = event.getGuiGraphics();
        drawContext.drawString(client.font, text, 4, 4, WHITE_COLOR, false);
    }
}
