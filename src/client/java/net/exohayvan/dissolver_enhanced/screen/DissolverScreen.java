package net.exohayvan.dissolver_enhanced.screen;

import java.util.Objects;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.PlayerDataClient;
import net.exohayvan.dissolver_enhanced.helpers.NumberHelpers;
import net.exohayvan.dissolver_enhanced.packets.DataSenderClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DissolverScreen extends AbstractContainerScreen<DissolverScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "textures/gui/dissolver_block_gui.png");
    
    // scroll
    private static final ResourceLocation SCROLLER_TEXTURE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_TEXTURE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private float scrollPosition;
    private boolean scrolling;

    // search
    private EditBox searchBox;
    private boolean ignoreTypedCharacter;

    public DissolverScreen(DissolverScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 217;
        this.imageHeight = 221;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // titleY = 1000;
        // this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
		this.inventoryLabelX = 31;
		this.inventoryLabelY = this.imageHeight - 93;
        
        // search box
        this.searchBox = new EditBox(this.font, this.leftPos + 104, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(16777215);
        // this.searchBox.setFocusUnlocked(false);
        this.searchBox.setFocused(true);
        this.addWidget(this.searchBox);
    }

    // DRAW

    int SCROLL_BAR_X = 198;
    int SCROLL_BAR_Y = 18;
    int MOUSE_SCROLL_AREA_WIDTH = 13;
    int MOUSE_SCROLL_AREA_HEIGHT = 106;
    int SCROLL_AREA_HEIGHT = 108;
    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // custom text
        context.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // search box
        this.searchBox.render(context, mouseX, mouseY, delta);

        // scroll bar
        int i = this.leftPos + SCROLL_BAR_X;
        int j = this.topPos + SCROLL_BAR_Y;
        int k = j + SCROLL_AREA_HEIGHT;
        boolean scrollActive = PlayerDataClient.LEARNED_ITEMS_SIZE > this.menu.CUSTOM_INV_SIZE;
        ResourceLocation scrollerTexture = scrollActive ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
        context.blitSprite(scrollerTexture, i, j + (int)((float)(k - j - 17) * this.scrollPosition), 12, 15);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        // context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        context.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        renderLearnedSummary(context);

        renderText(context, 33, 6);
    }

    private void renderLearnedSummary(GuiGraphics context) {
        String learnedSummary = getLearnedSummary();
        int summaryX = this.imageWidth - 19 - this.font.width(learnedSummary);
        context.drawString(this.font, learnedSummary, Math.max(84, summaryX), this.inventoryLabelY, 4210752, false);
    }

    private String getLearnedSummary() {
        int learned = PlayerDataClient.LEARNED_ITEMS_TOTAL_SIZE;
        int learnable = EMCValues.getLearnableCount();
        double percent = learnable == 0 ? 0 : learned * 100.0 / learnable;

        return learned + "/" + learnable + " (" + String.format("%.1f%%", percent) + ")";
    }

    private void renderText(GuiGraphics context, int x, int y) {
        // https://learn.microsoft.com/en-us/office/vba/api/word.wdcolor
        String MESSAGE = getMessage();
        context.drawString(this.font, MESSAGE, x, y, 16777215, false);
    }

    private String getMessage() {
        String CUSTOM_MSG = PlayerDataClient.MESSAGE;
        if (CUSTOM_MSG.isEmpty()) {
            if (PlayerDataClient.EMC.signum() == 0) {
                return Component.translatable("emc.empty").getString();
            }

            String emc = NumberHelpers.format(PlayerDataClient.EMC);
            Component text = Component.translatable("emc.title", emc);
            return text.getString();
        }

        Component text = CUSTOM_MSG.startsWith("literal:")
            ? Component.literal(CUSTOM_MSG.substring("literal:".length()))
            : Component.translatable(CUSTOM_MSG);
        return text.getString();
    }

    // SEARCH

    public boolean charTyped(char chr, int modifiers) {
        if (this.ignoreTypedCharacter) {
            return false;
        }
        
        String string = this.searchBox.getValue();
        // close screen if pressing "e" & nothing is searched
        if (string == "" && chr == "e".charAt(0)) {
            this.onClose();
            return false;
        }

        if (!this.searchBox.charTyped(chr, modifiers)) return false;
        
        if (!Objects.equals(string, this.searchBox.getValue())) {
            this.search();
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.ignoreTypedCharacter = false;
        
        boolean bl = this.hoveredSlot == null ? false : this.hoveredSlot.hasItem();
        boolean bl2 = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        if (bl && bl2 && this.checkHotbarKeyPressed(keyCode, scanCode)) {
            this.ignoreTypedCharacter = true;
            return true;
        } else {
            String string = this.searchBox.getValue();
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (!Objects.equals(string, this.searchBox.getValue())) {
                    this.search();
                }

                return true;
            }
            
            return this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256 ? true : super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.ignoreTypedCharacter = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void search() {
        String string = this.searchBox.getValue();
        DataSenderClient.sendDataToServer("search", string.isEmpty() ? "" : string);

        // reset scroll on search
        this.scrollPosition = 0.0F;
        DataSenderClient.sendDataToServer("scroll", "0");
    }

    // SCROLL    

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (mouseX >= leftPos + SCROLL_BAR_X && mouseY >= topPos + SCROLL_BAR_Y && mouseX < leftPos + SCROLL_BAR_X + MOUSE_SCROLL_AREA_WIDTH && mouseY < topPos + SCROLL_BAR_Y + MOUSE_SCROLL_AREA_HEIGHT) {
            this.scrolling = true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double verticalAmount) {
        // setting scroll when reaced end will clear all items, so just return
        if (this.scrollPosition == 0 && verticalAmount > 0 || this.scrollPosition == 1 && verticalAmount < 0) return false;

        this.scrollPosition = getScrollPosition(this.scrollPosition, verticalAmount);
        DataSenderClient.sendDataToServer("scroll", Float.toString(scrollPosition));

        return true;
	}

    protected float getScrollPosition(float current, double amount) {
        return Mth.clamp(current - (float)(amount / (double)this.getOverflowRows()), 0.0F, 1.0F);
    }

    protected int getOverflowRows() {
        return Mth.positiveCeilDiv(PlayerDataClient.LEARNED_ITEMS_SIZE, 9) - 6;
    }

	@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean scrollActive = PlayerDataClient.LEARNED_ITEMS_SIZE > this.menu.CUSTOM_INV_SIZE;

        if (!this.scrolling || !scrollActive) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

        float i = this.topPos + SCROLL_BAR_Y;
        float j = i + MOUSE_SCROLL_AREA_HEIGHT;
        scrollPosition = (float)(mouseY - i - 7.5F) / ((j - i) - 15.0F);
        scrollPosition = Mth.clamp(scrollPosition, 0.0F, 1F);
        DataSenderClient.sendDataToServer("scroll", Float.toString(scrollPosition));
        
        return true;
    }
}
