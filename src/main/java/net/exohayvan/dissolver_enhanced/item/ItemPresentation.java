package net.exohayvan.dissolver_enhanced.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class ItemPresentation {
    private ItemPresentation() {
    }

    static void appendGoldenTooltip(Consumer<Component> tooltip, String translationKey) {
        tooltip.accept(Component.translatable(translationKey).withStyle(ChatFormatting.GOLD));
    }
}
