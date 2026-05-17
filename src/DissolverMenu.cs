extern alias StardewGame;

using Microsoft.Xna.Framework;
using Microsoft.Xna.Framework.Graphics;
using ClickableComponent = StardewGame::StardewValley.Menus.ClickableComponent;
using Game1 = StardewGame::StardewValley.Game1;
using HUDMessage = StardewGame::StardewValley.HUDMessage;
using IClickableMenu = StardewGame::StardewValley.Menus.IClickableMenu;
using Item = StardewGame::StardewValley.Item;

namespace DissolverEnhanced.StardewValley.Smapi;

internal sealed class DissolverMenu : IClickableMenu
{
    private const int MenuWidth = 1200;
    private const int MenuHeight = 860;
    private const int MachineSlotSize = 84;
    private const int StoredSlotSize = 76;
    private const int InventorySlotSize = 72;
    private const int SlotGap = 8;
    private const int InventoryColumns = 12;
    private const int InventoryRows = 3;
    private const int DisplayedInventorySlots = InventoryColumns * InventoryRows;

    private readonly ClickableComponent dissolveSlot;
    private readonly ClickableComponent learnSlot;
    private readonly ClickableComponent unlearnSlot;
    private readonly List<ClickableComponent> storedSlots = new();
    private readonly List<ClickableComponent> inventorySlots = new();

    private Item? dissolveItem;
    private Item? learnItem;
    private Item? unlearnItem;
    private string hoverText = "";

    public DissolverMenu()
        : base(
            Math.Max(0, (Game1.uiViewport.Width - MenuWidth) / 2),
            Math.Max(0, (Game1.uiViewport.Height - MenuHeight) / 2),
            MenuWidth,
            MenuHeight,
            showUpperRightCloseButton: true
        )
    {
        int contentX = xPositionOnScreen + 64;
        int contentY = yPositionOnScreen + 104;
        int machinePanelX = xPositionOnScreen + width - 504;
        int machineSlotGap = 36;
        int machineGridWidth = MachineSlotSize * 3 + machineSlotGap * 2;
        int machineX = machinePanelX + (430 - machineGridWidth) / 2;

        dissolveSlot = new ClickableComponent(new Rectangle(machineX, contentY + 92, MachineSlotSize, MachineSlotSize), "Dissolve");
        learnSlot = new ClickableComponent(new Rectangle(machineX + MachineSlotSize + machineSlotGap, contentY + 92, MachineSlotSize, MachineSlotSize), "Learn");
        unlearnSlot = new ClickableComponent(new Rectangle(machineX + (MachineSlotSize + machineSlotGap) * 2, contentY + 92, MachineSlotSize, MachineSlotSize), "Unlearn");

        for (int row = 0; row < 4; row++)
        {
            for (int column = 0; column < 6; column++)
            {
                storedSlots.Add(new ClickableComponent(
                    new Rectangle(
                        contentX + column * (StoredSlotSize + SlotGap),
                        contentY + 76 + row * (StoredSlotSize + SlotGap),
                        StoredSlotSize,
                        StoredSlotSize
                    ),
                    $"Stored {row * 6 + column + 1}"
                ));
            }
        }

        int inventoryGridWidth = InventoryColumns * InventorySlotSize + (InventoryColumns - 1) * SlotGap;
        int inventoryX = xPositionOnScreen + (width - inventoryGridWidth) / 2;
        int inventoryY = yPositionOnScreen + 604;
        for (int index = 0; index < DisplayedInventorySlots; index++)
        {
            int column = index % InventoryColumns;
            int row = index / InventoryColumns;
            inventorySlots.Add(new ClickableComponent(
                new Rectangle(
                    inventoryX + column * (InventorySlotSize + SlotGap),
                    inventoryY + row * (InventorySlotSize + SlotGap),
                    InventorySlotSize,
                    InventorySlotSize
                ),
                $"Inventory {index + 1}"
            ));
        }
    }

    public override void gameWindowSizeChanged(Rectangle oldBounds, Rectangle newBounds)
    {
        Game1.activeClickableMenu = new DissolverMenu();
    }

    public override void receiveLeftClick(int x, int y, bool playSound = true)
    {
        base.receiveLeftClick(x, y, playSound);

        if (dissolveSlot.containsPoint(x, y))
        {
            SwapWithCursor(ref dissolveItem, "Dissolve slot");
            return;
        }

        if (learnSlot.containsPoint(x, y))
        {
            SwapWithCursor(ref learnItem, "Learn slot");
            return;
        }

        if (unlearnSlot.containsPoint(x, y))
        {
            SwapWithCursor(ref unlearnItem, "Unlearn slot");
            return;
        }

        for (int index = 0; index < inventorySlots.Count; index++)
        {
            if (inventorySlots[index].containsPoint(x, y))
            {
                if (IsInventorySlotUnlocked(index))
                {
                    SwapInventorySlotWithCursor(index);
                }
                return;
            }
        }
    }

    public override void receiveRightClick(int x, int y, bool playSound = true)
    {
        receiveLeftClick(x, y, playSound);
    }

    public override void performHoverAction(int x, int y)
    {
        hoverText = "";

        if (dissolveSlot.containsPoint(x, y))
        {
            hoverText = "Drop an item here to convert it into stored EMC.";
        }
        else if (learnSlot.containsPoint(x, y))
        {
            hoverText = "Drop an item here to teach the Dissolver.";
        }
        else if (unlearnSlot.containsPoint(x, y))
        {
            hoverText = "Drop an item here to unlearn it.";
        }
    }

    protected override void cleanupBeforeExit()
    {
        ReturnMachineItem(dissolveItem);
        ReturnMachineItem(learnItem);
        ReturnMachineItem(unlearnItem);
        dissolveItem = null;
        learnItem = null;
        unlearnItem = null;
        base.cleanupBeforeExit();
    }

    public override void draw(SpriteBatch b)
    {
        DrawPanel(b, new Rectangle(xPositionOnScreen, yPositionOnScreen, width, height), new Color(245, 221, 168), new Color(92, 57, 32));

        DrawTitle(b);
        DrawStoredGrid(b);
        DrawMachineSlots(b);
        DrawEmcPanel(b);
        DrawInventoryPanel(b);
        DrawHeldItem(b);

        if (!string.IsNullOrWhiteSpace(hoverText))
        {
            drawHoverText(b, hoverText, Game1.smallFont);
        }

        base.draw(b);
        drawMouse(b);
    }

    private void SwapWithCursor(ref Item? slotItem, string slotName)
    {
        Item? cursorItem = Game1.player.CursorSlotItem;
        Game1.player.CursorSlotItem = slotItem;
        slotItem = cursorItem;
        Game1.playSound("smallSelect");

        if (slotItem != null)
        {
            Game1.addHUDMessage(new HUDMessage($"{slotName} is staged. EMC behavior is next.", HUDMessage.newQuest_type));
        }
    }

    private static void SwapInventorySlotWithCursor(int slotIndex)
    {
        if (!IsInventorySlotUnlocked(slotIndex))
        {
            return;
        }

        Item? inventoryItem = Game1.player.Items[slotIndex];
        Game1.player.Items[slotIndex] = Game1.player.CursorSlotItem;
        Game1.player.CursorSlotItem = inventoryItem;
        Game1.playSound("smallSelect");
    }

    private static void ReturnMachineItem(Item? item)
    {
        if (item == null)
        {
            return;
        }

        Game1.player.addItemToInventoryBool(item, makeActiveObject: true);
    }

    private void DrawTitle(SpriteBatch b)
    {
        const string title = "Dissolver";
        Vector2 titleSize = Game1.dialogueFont.MeasureString(title);
        b.DrawString(
            Game1.dialogueFont,
            title,
            new Vector2(xPositionOnScreen + (width - titleSize.X) / 2, yPositionOnScreen + 24),
            Game1.textColor
        );
    }

    private void DrawStoredGrid(SpriteBatch b)
    {
        b.DrawString(Game1.dialogueFont, "Stored Items", new Vector2(xPositionOnScreen + 64, yPositionOnScreen + 92), Game1.textColor);
        foreach (ClickableComponent slot in storedSlots)
        {
            DrawSlot(b, slot.bounds, new Color(230, 211, 164));
        }
    }

    private void DrawMachineSlots(SpriteBatch b)
    {
        int panelX = xPositionOnScreen + width - 504;
        int panelY = yPositionOnScreen + 104;
        DrawPanel(b, new Rectangle(panelX, panelY, 430, 258), new Color(255, 244, 214), new Color(143, 99, 56));

        DrawLabeledSlot(b, dissolveSlot, "Dissolve", dissolveItem);
        DrawLabeledSlot(b, learnSlot, "Learn", learnItem);
        DrawLabeledSlot(b, unlearnSlot, "Unlearn", unlearnItem);
    }

    private void DrawEmcPanel(SpriteBatch b)
    {
        Rectangle panel = new(xPositionOnScreen + width - 504, yPositionOnScreen + 404, 430, 150);
        DrawPanel(b, panel, new Color(255, 244, 214), new Color(143, 99, 56));
        b.DrawString(Game1.smallFont, "Stored EMC: 0", new Vector2(panel.X + 24, panel.Y + 18), Game1.textColor);
        b.DrawString(Game1.smallFont, "Selected EMC: -", new Vector2(panel.X + 24, panel.Y + 50), Game1.textColor);
        b.DrawString(Game1.smallFont, "Stack Total: -", new Vector2(panel.X + 24, panel.Y + 82), Game1.textColor);
        b.DrawString(Game1.smallFont, "Learned: 0", new Vector2(panel.X + 24, panel.Y + 114), Game1.textColor);
    }

    private void DrawInventoryPanel(SpriteBatch b)
    {
        Rectangle panel = new(xPositionOnScreen + 64, yPositionOnScreen + 548, width - 128, 286);
        DrawPanel(b, panel, new Color(255, 244, 214), new Color(143, 99, 56));
        b.DrawString(Game1.dialogueFont, "Inventory", new Vector2(panel.X + 20, panel.Y + 14), Game1.textColor);

        for (int index = 0; index < inventorySlots.Count; index++)
        {
            ClickableComponent slot = inventorySlots[index];
            bool unlocked = IsInventorySlotUnlocked(index);
            DrawSlot(b, slot.bounds, unlocked ? new Color(244, 213, 164) : new Color(142, 126, 101));
            if (unlocked)
            {
                DrawItem(b, Game1.player.Items[index], slot.bounds);
            }
            else
            {
                DrawLockedSlotOverlay(b, slot.bounds);
            }
        }
    }

    private static void DrawLabeledSlot(SpriteBatch b, ClickableComponent slot, string label, Item? item)
    {
        Vector2 labelSize = Game1.smallFont.MeasureString(label);
        b.DrawString(Game1.smallFont, label, new Vector2(slot.bounds.Center.X - labelSize.X / 2, slot.bounds.Y - 28), Game1.textColor);
        DrawSlot(b, slot.bounds, new Color(230, 211, 164));
        DrawItem(b, item, slot.bounds);
    }

    private static void DrawItem(SpriteBatch b, Item? item, Rectangle slot)
    {
        const float itemDrawSize = 64f;
        Vector2 position = new(
            slot.X + (slot.Width - itemDrawSize) / 2f,
            slot.Y + (slot.Height - itemDrawSize) / 2f
        );
        item?.drawInMenu(b, position, 1f);
    }

    private static void DrawHeldItem(SpriteBatch b)
    {
        Item? held = Game1.player.CursorSlotItem;
        if (held == null)
        {
            return;
        }

        held.drawInMenu(b, new Vector2(Game1.getOldMouseX() + 8, Game1.getOldMouseY() + 8), 1f);
    }

    private static bool IsInventorySlotUnlocked(int slotIndex)
    {
        return slotIndex >= 0 && slotIndex < Game1.player.Items.Count;
    }

    private static void DrawLockedSlotOverlay(SpriteBatch b, Rectangle slot)
    {
        b.Draw(Game1.staminaRect, new Rectangle(slot.X + 8, slot.Y + 8, slot.Width - 16, slot.Height - 16), Color.Black * 0.18f);
        b.Draw(Game1.staminaRect, new Rectangle(slot.X + 16, slot.Y + slot.Height / 2 - 3, slot.Width - 32, 6), new Color(70, 57, 45) * 0.45f);
    }

    private static void DrawSlot(SpriteBatch b, Rectangle bounds, Color fill)
    {
        DrawPanel(b, bounds, fill, new Color(102, 72, 43));
        b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 5, bounds.Y + 5, bounds.Width - 10, bounds.Height - 10), new Color(56, 43, 35) * 0.18f);
    }

    private static void DrawPanel(SpriteBatch b, Rectangle bounds, Color fill, Color border)
    {
        b.Draw(Game1.staminaRect, bounds, border);
        b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 4, bounds.Y + 4, bounds.Width - 8, bounds.Height - 8), fill);
    }
}
