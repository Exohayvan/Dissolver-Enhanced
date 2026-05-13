package net.exohayvan.dissolver_enhanced.screen;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;
import net.exohayvan.dissolver_enhanced.packets.DataSenderClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DissolverScreen extends AbstractContainerScreen<DissolverScreenHandler> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "textures/gui/dissolver_block_gui.png");
    private static final Identifier SCROLLER_TEXTURE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    private static final int SCROLL_BAR_X = 198;
    private static final int SCROLL_BAR_Y = 18;
    private static final int SCROLL_AREA_HEIGHT = 108;
    private static final int SEARCH_X = 104;
    private static final int SEARCH_Y = 6;
    private static final int SEARCH_WIDTH = 80;
    private static final int SEARCH_HEIGHT = 9;
    private static final int MESSAGE_X = 33;
    private static final int MESSAGE_Y = 6;

    private float scrollPosition;
    private EditBox searchBox;

    public DissolverScreen(DissolverScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 217, 221);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 31;
        this.inventoryLabelY = this.imageHeight - 93;

        this.searchBox = new EditBox(this.font, this.leftPos + SEARCH_X, this.topPos + SEARCH_Y, SEARCH_WIDTH, SEARCH_HEIGHT, Component.translatable("gui.dissolver_enhanced.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(50);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setCanLoseFocus(false);
        this.searchBox.setFocused(true);
        this.setFocused(this.searchBox);
        this.searchBox.setResponder(value -> search());
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.searchBox != null && this.searchBox.isMouseOver(event.x(), event.y())) {
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            return this.searchBox.mouseClicked(event, doubleClick);
        }

        if (this.searchBox != null) {
            this.searchBox.setFocused(false);
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.keyPressed(event)) {
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.charTyped(event)) {
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractForeground(graphics);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        boolean scrollActive = PlayerDataClient.LEARNED_ITEMS_SIZE > this.menu.CUSTOM_INV_SIZE;
        Identifier scrollerTexture = scrollActive ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
        int scrollX = this.leftPos + SCROLL_BAR_X;
        int scrollY = this.topPos + SCROLL_BAR_Y + (int)((SCROLL_AREA_HEIGHT - 17) * this.scrollPosition);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scrollerTexture, scrollX, scrollY, 12, 15);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        String learnedSummary = getLearnedSummary();
        int summaryX = this.imageWidth - 19 - this.font.width(learnedSummary);
        graphics.text(this.font, learnedSummary, Math.max(84, summaryX), this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((this.scrollPosition == 0 && scrollY > 0) || (this.scrollPosition == 1 && scrollY < 0)) return false;

        this.scrollPosition = Mth.clamp(this.scrollPosition - (float)(scrollY / (double)this.getOverflowRows()), 0.0F, 1.0F);
        DataSenderClient.sendDataToServer("scroll", Float.toString(this.scrollPosition));
        return true;
    }

    private Component getMessage() {
        String customMessage = PlayerDataClient.MESSAGE;
        if (customMessage.isEmpty()) {
            if (PlayerDataClient.EMC.signum() == 0) {
                return Component.translatable("emc.empty");
            }

            return Component.literal("EMC: ").append(Component.literal(NumberHelpers.format(PlayerDataClient.EMC)).withStyle(ChatFormatting.GOLD));
        }

        if (customMessage.startsWith("literal:")) {
            return literalMessage(customMessage.substring("literal:".length()));
        }

        return Component.translatable(customMessage);
    }

    private Component literalMessage(String message) {
        if (message.startsWith("§a")) {
            return Component.literal(message.substring(2)).withStyle(ChatFormatting.GREEN);
        }

        if (message.startsWith("§c")) {
            return Component.literal(message.substring(2)).withStyle(ChatFormatting.RED);
        }

        return Component.literal(message);
    }

    private String getLearnedSummary() {
        int learned = PlayerDataClient.LEARNED_ITEMS_TOTAL_SIZE;
        int learnable = EMCValues.getLearnableCount();
        double percent = learnable == 0 ? 0 : learned * 100.0 / learnable;

        return learned + "/" + learnable + " (" + String.format("%.1f%%", percent) + ")";
    }

    private int getOverflowRows() {
        return Math.max(1, Mth.ceil((float)PlayerDataClient.LEARNED_ITEMS_SIZE / 9.0F) - 6);
    }

    private void search() {
        String value = this.searchBox.getValue();
        DataSenderClient.sendDataToServer("search", value.isEmpty() ? "" : value);

        this.scrollPosition = 0.0F;
        DataSenderClient.sendDataToServer("scroll", "0");
    }

    private void extractForeground(GuiGraphicsExtractor graphics) {
        graphics.text(this.font, getMessage(), this.leftPos + MESSAGE_X, this.topPos + MESSAGE_Y, 0xFFFFFFFF, false);
    }
}
