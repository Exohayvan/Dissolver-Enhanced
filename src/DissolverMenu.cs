extern alias StardewGame;
extern alias MonoGame;

using DissolverEnhanced.StardewValley.Common.Analytics;
using Color = MonoGame::Microsoft.Xna.Framework.Color;
using Rectangle = MonoGame::Microsoft.Xna.Framework.Rectangle;
using SpriteBatch = MonoGame::Microsoft.Xna.Framework.Graphics.SpriteBatch;
using Vector2 = MonoGame::Microsoft.Xna.Framework.Vector2;
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
    private const int MaxExtractStack = 999;
    private const long HeldExtractDelayMs = 90;

    private readonly ClickableComponent dissolveSlot;
    private readonly ClickableComponent learnSlot;
    private readonly ClickableComponent unlearnSlot;
    private readonly ClickableComponent previousLearnedPageButton;
    private readonly ClickableComponent nextLearnedPageButton;
    private readonly List<ClickableComponent> storedSlots = new();
    private readonly List<ClickableComponent> inventorySlots = new();
    private readonly StardewModAnalytics? analytics;

    private Item? hoveredItem;
    private string hoverText = "";
    private int learnedPage;
    private long nextHeldExtractAtMs;

    public DissolverMenu(StardewModAnalytics? analytics)
        : base(
            Math.Max(0, (Game1.uiViewport.Width - MenuWidth) / 2),
            Math.Max(0, (Game1.uiViewport.Height - MenuHeight) / 2),
            MenuWidth,
            MenuHeight,
            showUpperRightCloseButton: true
        )
    {
        this.analytics = analytics;
        int contentX = xPositionOnScreen + 64;
        int contentY = yPositionOnScreen + 104;
        int machinePanelX = xPositionOnScreen + width - 504;
        int machineSlotGap = 36;
        int machineGridWidth = MachineSlotSize * 3 + machineSlotGap * 2;
        int machineX = machinePanelX + (430 - machineGridWidth) / 2;

        dissolveSlot = new ClickableComponent(new Rectangle(machineX, contentY + 92, MachineSlotSize, MachineSlotSize), "Dissolve");
        learnSlot = new ClickableComponent(new Rectangle(machineX + MachineSlotSize + machineSlotGap, contentY + 92, MachineSlotSize, MachineSlotSize), "Learn");
        unlearnSlot = new ClickableComponent(new Rectangle(machineX + (MachineSlotSize + machineSlotGap) * 2, contentY + 92, MachineSlotSize, MachineSlotSize), "Unlearn");
        previousLearnedPageButton = new ClickableComponent(new Rectangle(contentX + 320, contentY - 10, 44, 44), "Previous learned page");
        nextLearnedPageButton = new ClickableComponent(new Rectangle(contentX + 374, contentY - 10, 44, 44), "Next learned page");

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
        Game1.activeClickableMenu = new DissolverMenu(analytics);
    }

    public override void receiveLeftClick(int x, int y, bool playSound = true)
    {
        base.receiveLeftClick(x, y, playSound);

        if (dissolveSlot.containsPoint(x, y))
        {
            DissolveCursorItem();
            return;
        }

        if (learnSlot.containsPoint(x, y))
        {
            LearnCursorItem();
            return;
        }

        if (unlearnSlot.containsPoint(x, y))
        {
            UnlearnCursorItem();
            return;
        }

        if (previousLearnedPageButton.containsPoint(x, y))
        {
            ChangeLearnedPage(-1);
            return;
        }

        if (nextLearnedPageButton.containsPoint(x, y))
        {
            ChangeLearnedPage(1);
            return;
        }

        if (TryGetLearnedItemAt(x, y, out string? learnedItem))
        {
            if (learnedItem != null)
            {
                ExtractLearnedItem(learnedItem, 1);
            }

            return;
        }

        for (int index = 0; index < inventorySlots.Count; index++)
        {
            if (inventorySlots[index].containsPoint(x, y))
            {
                if (IsInventorySlotUnlocked(index))
                {
                    HandleInventorySlotLeftClick(index);
                }
                return;
            }
        }
    }

    public override void receiveRightClick(int x, int y, bool playSound = true)
    {
        if (dissolveSlot.containsPoint(x, y))
        {
            DissolveCursorItem();
            return;
        }

        if (learnSlot.containsPoint(x, y))
        {
            LearnCursorItem();
            return;
        }

        if (unlearnSlot.containsPoint(x, y))
        {
            UnlearnCursorItem();
            return;
        }

        if (TryGetLearnedItemAt(x, y, out string? learnedItem))
        {
            if (learnedItem != null)
            {
                ExtractLearnedItem(learnedItem, MaxExtractStack);
            }

            return;
        }

        for (int index = 0; index < inventorySlots.Count; index++)
        {
            if (inventorySlots[index].containsPoint(x, y))
            {
                if (IsInventorySlotUnlocked(index))
                {
                    HandleInventorySlotRightClick(index);
                }
                return;
            }
        }
    }

    public override void leftClickHeld(int x, int y)
    {
        if (Environment.TickCount64 < nextHeldExtractAtMs)
        {
            return;
        }

        if (TryGetLearnedItemAt(x, y, out string? learnedItem))
        {
            if (learnedItem != null)
            {
                ExtractLearnedItem(learnedItem, 1);
                nextHeldExtractAtMs = Environment.TickCount64 + HeldExtractDelayMs;
            }

            return;
        }
    }

    public override void performHoverAction(int x, int y)
    {
        hoverText = "";
        hoveredItem = null;

        if (dissolveSlot.containsPoint(x, y))
        {
            hoveredItem = Game1.player.CursorSlotItem;
            hoverText = ItemTooltip(hoveredItem, "Hold an item and click to convert it into stored EMC.");
        }
        else if (learnSlot.containsPoint(x, y))
        {
            hoveredItem = Game1.player.CursorSlotItem;
            hoverText = ItemTooltip(hoveredItem, "Hold an item and click to teach the Dissolver.");
        }
        else if (unlearnSlot.containsPoint(x, y))
        {
            hoveredItem = Game1.player.CursorSlotItem;
            hoverText = ItemTooltip(hoveredItem, "Hold an item and click to unlearn it.");
        }
        else
        {
            if (TryGetLearnedItemAt(x, y, out string? learnedItem))
            {
                if (learnedItem != null)
                {
                    hoveredItem = EmcValueRegistry.CreateItem(learnedItem);
                    hoverText = ItemTooltip(hoveredItem, "Learned item.");
                }

                return;
            }

            for (int index = 0; index < inventorySlots.Count; index++)
            {
                if (!inventorySlots[index].containsPoint(x, y))
                {
                    continue;
                }

                if (IsInventorySlotUnlocked(index))
                {
                    hoveredItem = Game1.player.Items[index];
                    hoverText = ItemTooltip(hoveredItem, "Inventory item.");
                }
                else
                {
                    hoverText = "Locked backpack slot.";
                }

                return;
            }
        }
    }

    protected override void cleanupBeforeExit()
    {
        base.cleanupBeforeExit();
    }

    public override void draw(SpriteBatch b)
    {
        DrawPanel(b, new Rectangle(xPositionOnScreen, yPositionOnScreen, width, height), new Color(246, 185, 103), new Color(111, 57, 24));

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

    private void DissolveCursorItem()
    {
        Item? item = Game1.player.CursorSlotItem;
        if (item == null)
        {
            return;
        }

        long totalValue = StackEmcValue(item);
        string? itemId = EmcValueRegistry.QualifiedItemId(item);
        bool learned = false;
        if (!string.IsNullOrWhiteSpace(itemId))
        {
            learned = DissolverState.Learn(itemId);
        }

        DissolverState.AddEmc(totalValue);
        CaptureItemDissolved(item, itemId, totalValue);
        if (learned)
        {
            CaptureItemLearned(item, itemId, totalValue);
        }

        Game1.player.CursorSlotItem = null;
        Game1.playSound("smallSelect");
        Game1.addHUDMessage(new HUDMessage($"Dissolved {item.DisplayName} for {totalValue} EMC.", HUDMessage.newQuest_type));
    }

    private void LearnCursorItem()
    {
        Item? item = Game1.player.CursorSlotItem;
        string? itemId = item == null ? null : EmcValueRegistry.QualifiedItemId(item);
        if (item == null || string.IsNullOrWhiteSpace(itemId))
        {
            return;
        }

        bool learned = DissolverState.Learn(itemId);
        if (learned)
        {
            CaptureItemLearned(item, itemId, EmcValue(item));
        }
        else
        {
            CaptureItemRejected(item, itemId, "already_learned");
        }

        Game1.playSound(learned ? "newArtifact" : "smallSelect");
        Game1.addHUDMessage(new HUDMessage(
            learned ? $"Learned {item.DisplayName}." : $"{item.DisplayName} is already learned.",
            HUDMessage.newQuest_type
        ));
    }

    private void ExtractLearnedItem(string itemId, int requestedStack)
    {
        Item? cursorItem = Game1.player.CursorSlotItem;
        if (cursorItem != null)
        {
            string? cursorItemId = EmcValueRegistry.QualifiedItemId(cursorItem);
            if (!string.Equals(cursorItemId, itemId, StringComparison.OrdinalIgnoreCase) || StackRoom(cursorItem) <= 0)
            {
                CaptureItemRejected(cursorItem, cursorItemId, "cursor_not_clear_or_not_stackable");
                Game1.addHUDMessage(new HUDMessage("Clear your cursor or hold the same stackable item before extracting.", HUDMessage.newQuest_type));
                return;
            }
        }

        int itemValue = EmcValueRegistry.EmcValue(itemId);
        int stack = Math.Max(1, Math.Min(MaxExtractStack, requestedStack));
        if (cursorItem != null)
        {
            stack = Math.Min(stack, StackRoom(cursorItem));
        }

        if (itemValue > 0)
        {
            stack = (int)Math.Min(stack, DissolverState.StoredEmc / itemValue);
        }

        if (stack <= 0)
        {
            CaptureItemRejected(itemId, "not_enough_emc");
            Game1.addHUDMessage(new HUDMessage($"Not enough EMC. Need {itemValue}.", HUDMessage.newQuest_type));
            return;
        }

        long totalCost = (long)itemValue * stack;
        if (!DissolverState.TrySpendEmc(totalCost))
        {
            CaptureItemRejected(itemId, "not_enough_emc");
            Game1.addHUDMessage(new HUDMessage($"Not enough EMC. Need {totalCost}.", HUDMessage.newQuest_type));
            return;
        }

        Item? item = EmcValueRegistry.CreateItem(itemId, stack);
        if (item == null)
        {
            DissolverState.AddEmc(totalCost);
            CaptureItemRejected(itemId, "create_item_failed");
            Game1.addHUDMessage(new HUDMessage($"Could not create {itemId}.", HUDMessage.newQuest_type));
            return;
        }

        if (cursorItem == null)
        {
            Game1.player.CursorSlotItem = item;
        }
        else
        {
            SetStack(cursorItem, cursorItem.Stack + stack);
        }

        CaptureItemExtracted(item, itemId, totalCost);
        Game1.playSound("smallSelect");
    }

    private void UnlearnCursorItem()
    {
        Item? item = Game1.player.CursorSlotItem;
        string? itemId = item == null ? null : EmcValueRegistry.QualifiedItemId(item);
        if (item == null || string.IsNullOrWhiteSpace(itemId))
        {
            return;
        }

        bool removed = DissolverState.Unlearn(itemId);
        if (!removed)
        {
            CaptureItemRejected(item, itemId, "not_learned");
        }

        Game1.playSound(removed ? "trashcan" : "smallSelect");
        Game1.addHUDMessage(new HUDMessage(
            removed ? $"Unlearned {item.DisplayName}." : $"{item.DisplayName} was not learned.",
            HUDMessage.newQuest_type
        ));
    }

    private void CaptureItemLearned(Item item, string? itemId, long totalValue)
    {
        CaptureItemEvent((namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem) =>
            analytics?.CaptureDissolverItemLearned(namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem),
            item,
            itemId,
            totalValue
        );
    }

    private void CaptureItemDissolved(Item item, string? itemId, long totalValue)
    {
        CaptureItemEvent((namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem) =>
            analytics?.CaptureDissolverItemDissolved(namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem),
            item,
            itemId,
            totalValue
        );
    }

    private void CaptureItemExtracted(Item item, string? itemId, long totalValue)
    {
        CaptureItemEvent((namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem) =>
            analytics?.CaptureDissolverItemExtracted(namespaceId, itemName, qualifiedItemId, stackCount, singleValue, eventTotalValue, creativeItem),
            item,
            itemId,
            totalValue
        );
    }

    private void CaptureItemEvent(
        Action<string, string, string, int, string, string, bool> capture,
        Item item,
        string? itemId,
        long totalValue
    )
    {
        if (string.IsNullOrWhiteSpace(itemId))
        {
            return;
        }

        string namespaceId = Namespace(itemId);
        string itemName = ItemName(itemId);
        int stackCount = Math.Max(1, item.Stack);
        string singleValue = EmcValue(item).ToString();
        capture(namespaceId, itemName, AnalyticsItemId(itemId, item.DisplayName), stackCount, singleValue, totalValue.ToString(), IsCreativeItem(itemId));
    }

    private void CaptureItemRejected(Item item, string? itemId, string reason)
    {
        if (string.IsNullOrWhiteSpace(itemId))
        {
            return;
        }

        analytics?.CaptureDissolverItemRejected(Namespace(itemId), ItemName(itemId), AnalyticsItemId(itemId, item.DisplayName), reason);
    }

    private void CaptureItemRejected(string itemId, string reason)
    {
        analytics?.CaptureDissolverItemRejected(Namespace(itemId), ItemName(itemId), AnalyticsItemId(itemId, null), reason);
    }

    private static string AnalyticsItemId(string itemId, string? displayName)
    {
        if (string.IsNullOrWhiteSpace(displayName))
        {
            return itemId;
        }

        return itemId + ":" + displayName.Trim();
    }

    private static string Namespace(string itemId)
    {
        string normalized = NormalizeQualifiedItemId(itemId);
        int namespaceEnd = normalized.IndexOf(':');
        return namespaceEnd > 0 ? normalized[..namespaceEnd] : "stardew_valley";
    }

    private static string ItemName(string itemId)
    {
        string normalized = NormalizeQualifiedItemId(itemId);
        int namespaceEnd = normalized.IndexOf(':');
        return namespaceEnd > 0 ? normalized[(namespaceEnd + 1)..] : normalized;
    }

    private static string NormalizeQualifiedItemId(string itemId)
    {
        string normalized = itemId.Trim();
        if (normalized.Length > 3 && normalized[0] == '(')
        {
            int prefixEnd = normalized.IndexOf(')');
            if (prefixEnd >= 0 && prefixEnd + 1 < normalized.Length)
            {
                normalized = normalized[(prefixEnd + 1)..];
            }
        }

        return normalized;
    }

    private static bool IsCreativeItem(string itemId)
    {
        string normalized = NormalizeQualifiedItemId(itemId);
        return normalized.Contains("debug", StringComparison.OrdinalIgnoreCase)
            || normalized.Contains("error", StringComparison.OrdinalIgnoreCase);
    }

    private void ChangeLearnedPage(int delta)
    {
        int maxPage = MaxLearnedPage();
        int nextPage = Math.Clamp(learnedPage + delta, 0, maxPage);
        if (nextPage == learnedPage)
        {
            return;
        }

        learnedPage = nextPage;
        Game1.playSound("shwip");
    }

    private int MaxLearnedPage()
    {
        int learnedCount = DissolverState.LearnedCount;
        return Math.Max(0, (learnedCount - 1) / storedSlots.Count);
    }

    private bool TryGetLearnedItemAt(int x, int y, out string? itemId)
    {
        itemId = null;
        IReadOnlyList<string> learnedItems = DissolverState.LearnedItems;
        for (int index = 0; index < storedSlots.Count; index++)
        {
            if (!storedSlots[index].containsPoint(x, y))
            {
                continue;
            }

            int learnedIndex = LearnedItemIndex(index);
            if (learnedIndex < learnedItems.Count)
            {
                itemId = learnedItems[learnedIndex];
            }

            return true;
        }

        return false;
    }

    private int LearnedItemIndex(int slotIndex)
    {
        learnedPage = Math.Min(learnedPage, MaxLearnedPage());
        return learnedPage * storedSlots.Count + slotIndex;
    }

    private static void HandleInventorySlotLeftClick(int slotIndex)
    {
        if (!IsInventorySlotUnlocked(slotIndex))
        {
            return;
        }

        Item? inventoryItem = Game1.player.Items[slotIndex];
        Item? cursorItem = Game1.player.CursorSlotItem;
        if (TryCombineStacks(cursorItem, inventoryItem))
        {
            if (cursorItem != null && StackOf(cursorItem) <= 0)
            {
                Game1.player.CursorSlotItem = null;
            }
        }
        else
        {
            Game1.player.Items[slotIndex] = cursorItem;
            Game1.player.CursorSlotItem = inventoryItem;
        }

        Game1.playSound("smallSelect");
    }

    private static void HandleInventorySlotRightClick(int slotIndex)
    {
        if (!IsInventorySlotUnlocked(slotIndex))
        {
            return;
        }

        Item? inventoryItem = Game1.player.Items[slotIndex];
        Item? cursorItem = Game1.player.CursorSlotItem;

        if (cursorItem != null)
        {
            if (inventoryItem == null)
            {
                Game1.player.Items[slotIndex] = TakeOneFromCursor();
                Game1.playSound("smallSelect");
            }
            else if (TryCombineOne(cursorItem, inventoryItem) && cursorItem.Stack <= 0)
            {
                Game1.player.CursorSlotItem = null;
                Game1.playSound("smallSelect");
            }

            return;
        }

        if (inventoryItem == null)
        {
            return;
        }

        int taken = Math.Max(1, inventoryItem.Stack / 2);
        Game1.player.CursorSlotItem = CloneStack(inventoryItem, taken);
        SetStack(inventoryItem, inventoryItem.Stack - taken);
        if (StackOf(inventoryItem) <= 0)
        {
            Game1.player.Items[slotIndex] = null;
        }

        Game1.playSound("smallSelect");
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
        IReadOnlyList<string> learnedItems = DissolverState.LearnedItems;
        learnedPage = Math.Min(learnedPage, MaxLearnedPage());
        DrawPageButton(b, previousLearnedPageButton.bounds, "<", learnedPage > 0);
        DrawPageButton(b, nextLearnedPageButton.bounds, ">", learnedPage < MaxLearnedPage());
        string pageText = $"Page {learnedPage + 1}/{MaxLearnedPage() + 1}";
        b.DrawString(Game1.smallFont, pageText, new Vector2(xPositionOnScreen + 64, yPositionOnScreen + 132), Game1.textColor);
        for (int index = 0; index < storedSlots.Count; index++)
        {
            ClickableComponent slot = storedSlots[index];
            DrawSlot(b, slot.bounds, new Color(241, 199, 132));
            int learnedIndex = LearnedItemIndex(index);
            if (learnedIndex < learnedItems.Count)
            {
                DrawItem(b, EmcValueRegistry.CreateItem(learnedItems[learnedIndex]), slot.bounds);
            }
        }
    }

    private void DrawMachineSlots(SpriteBatch b)
    {
        int panelX = xPositionOnScreen + width - 504;
        int panelY = yPositionOnScreen + 104;
        DrawPanel(b, new Rectangle(panelX, panelY, 430, 258), new Color(255, 204, 128), new Color(139, 70, 25));

        DrawLabeledSlot(b, dissolveSlot, "Dissolve");
        DrawLabeledSlot(b, learnSlot, "Learn");
        DrawLabeledSlot(b, unlearnSlot, "Unlearn");
    }

    private void DrawEmcPanel(SpriteBatch b)
    {
        Rectangle panel = new(xPositionOnScreen + width - 504, yPositionOnScreen + 404, 430, 150);
        DrawPanel(b, panel, new Color(255, 204, 128), new Color(139, 70, 25));
        int totalItems = EmcValueRegistry.ItemCount;
        int learnedCount = DissolverState.LearnedCount;
        string learnedPercent = totalItems <= 0 ? "0%" : $"{learnedCount * 100.0 / totalItems:0.#}%";
        b.DrawString(Game1.smallFont, $"Stored EMC: {DissolverState.StoredEmc}", new Vector2(panel.X + 24, panel.Y + 18), Game1.textColor);
        b.DrawString(Game1.smallFont, $"Learned: {learnedCount}/{totalItems} items ({learnedPercent})", new Vector2(panel.X + 24, panel.Y + 58), Game1.textColor);
    }

    private void DrawInventoryPanel(SpriteBatch b)
    {
        Rectangle panel = new(xPositionOnScreen + 64, yPositionOnScreen + 548, width - 128, 286);
        DrawPanel(b, panel, new Color(255, 204, 128), new Color(139, 70, 25));
        b.DrawString(Game1.dialogueFont, "Inventory", new Vector2(panel.X + 20, panel.Y + 14), Game1.textColor);

        for (int index = 0; index < inventorySlots.Count; index++)
        {
            ClickableComponent slot = inventorySlots[index];
            bool unlocked = IsInventorySlotUnlocked(index);
            DrawSlot(b, slot.bounds, unlocked ? new Color(255, 205, 126) : new Color(126, 91, 57));
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

    private static void DrawLabeledSlot(SpriteBatch b, ClickableComponent slot, string label)
    {
        Vector2 labelSize = Game1.smallFont.MeasureString(label);
        b.DrawString(Game1.smallFont, label, new Vector2(slot.bounds.Center.X - labelSize.X / 2, slot.bounds.Y - 28), Game1.textColor);
        DrawSlot(b, slot.bounds, new Color(241, 199, 132));
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

    private static string ItemTooltip(Item? item, string emptyText)
    {
        if (item == null)
        {
            return emptyText;
        }

        int emcValue = EmcValue(item);
        int stack = Math.Max(1, item.Stack);
        string? itemId = EmcValueRegistry.QualifiedItemId(item);
        List<string> lines = new()
        {
            item.DisplayName,
            $"EMC: {emcValue}"
        };
        if (stack > 1)
        {
            lines.Add($"Stack EMC: {StackEmcValue(item)}");
        }

        if (!string.IsNullOrWhiteSpace(itemId) && DissolverState.IsLearned(itemId))
        {
            lines.Add("Learned");
        }

        return string.Join('\n', lines);
    }

    private static int SellPrice(Item item)
    {
        return Math.Max(0, item.sellToStorePrice(-1));
    }

    private static int EmcValue(Item item)
    {
        return EmcValueRegistry.EmcValue(item);
    }

    private static long StackEmcValue(Item item)
    {
        return (long)EmcValue(item) * Math.Max(1, item.Stack);
    }

    private static bool IsInventorySlotUnlocked(int slotIndex)
    {
        return slotIndex >= 0 && slotIndex < Game1.player.Items.Count;
    }

    private static bool TryCombineStacks(Item? source, Item? target)
    {
        if (source == null || target == null || !CanStackWith(source, target))
        {
            return false;
        }

        int room = StackRoom(target);
        if (room <= 0)
        {
            return false;
        }

        int moved = Math.Min(room, source.Stack);
        SetStack(target, target.Stack + moved);
        SetStack(source, source.Stack - moved);
        return moved > 0;
    }

    private static bool TryCombineOne(Item source, Item target)
    {
        if (!CanStackWith(source, target) || source.Stack <= 0)
        {
            return false;
        }

        if (StackRoom(target) <= 0)
        {
            return false;
        }

        SetStack(target, target.Stack + 1);
        SetStack(source, source.Stack - 1);
        return true;
    }

    private static bool CanStackWith(Item source, Item target)
    {
        if (!GameAllowsStacking(source, target))
        {
            return false;
        }

        string? sourceId = EmcValueRegistry.QualifiedItemId(source);
        string? targetId = EmcValueRegistry.QualifiedItemId(target);
        return !string.IsNullOrWhiteSpace(sourceId)
            && string.Equals(sourceId, targetId, StringComparison.OrdinalIgnoreCase);
    }

    private static bool GameAllowsStacking(Item source, Item target)
    {
        foreach (string methodName in new[] { "canStackWith", "CanStackWith" })
        {
            foreach (System.Reflection.MethodInfo method in source.GetType().GetMethods())
            {
                System.Reflection.ParameterInfo[] parameters = method.GetParameters();
                if (!method.Name.Equals(methodName, StringComparison.Ordinal) || parameters.Length != 1)
                {
                    continue;
                }

                if (!parameters[0].ParameterType.IsInstanceOfType(target))
                {
                    continue;
                }

                if (method.Invoke(source, new object[] { target }) is bool canStack)
                {
                    return canStack;
                }
            }
        }

        return true;
    }

    private static int StackRoom(Item item)
    {
        return Math.Max(0, MaxStackSize(item) - item.Stack);
    }

    private static int MaxStackSize(Item item)
    {
        foreach (string memberName in new[] { "maximumStackSize", "MaximumStackSize", "maxStackSize", "MaxStackSize" })
        {
            object? value = item.GetType().GetMethod(memberName, Type.EmptyTypes)?.Invoke(item, Array.Empty<object>())
                ?? item.GetType().GetProperty(memberName)?.GetValue(item)
                ?? item.GetType().GetField(memberName)?.GetValue(item);
            if (value is int maxStack && maxStack > 0)
            {
                return maxStack;
            }
        }

        return item.Stack <= 1 ? 1 : MaxExtractStack;
    }

    private static Item? TakeOneFromCursor()
    {
        Item? cursorItem = Game1.player.CursorSlotItem;
        if (cursorItem == null)
        {
            return null;
        }

        Item? single = CloneStack(cursorItem, 1);
        SetStack(cursorItem, cursorItem.Stack - 1);
        if (StackOf(cursorItem) <= 0)
        {
            Game1.player.CursorSlotItem = null;
        }

        return single;
    }

    private static Item? CloneStack(Item item, int stack)
    {
        string? itemId = EmcValueRegistry.QualifiedItemId(item);
        return string.IsNullOrWhiteSpace(itemId) ? null : EmcValueRegistry.CreateItem(itemId, stack);
    }

    private static int StackOf(Item item)
    {
        return item.Stack;
    }

    private static void SetStack(Item item, int stack)
    {
        item.GetType().GetProperty("Stack")?.SetValue(item, Math.Max(0, stack));
    }

    private static void DrawLockedSlotOverlay(SpriteBatch b, Rectangle slot)
    {
        b.Draw(Game1.staminaRect, new Rectangle(slot.X + 8, slot.Y + 8, slot.Width - 16, slot.Height - 16), Color.Black * 0.18f);
        b.Draw(Game1.staminaRect, new Rectangle(slot.X + 16, slot.Y + slot.Height / 2 - 3, slot.Width - 32, 6), new Color(70, 57, 45) * 0.45f);
    }

    private static void DrawSlot(SpriteBatch b, Rectangle bounds, Color fill)
    {
        b.Draw(Game1.staminaRect, bounds, new Color(114, 69, 31));
        b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 4, bounds.Y + 4, bounds.Width - 8, bounds.Height - 8), new Color(188, 98, 33));
        b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 8, bounds.Y + 8, bounds.Width - 16, bounds.Height - 16), fill);
        b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 9, bounds.Y + 9, bounds.Width - 18, bounds.Height - 18), new Color(255, 255, 255) * 0.08f);
    }

    private static void DrawPageButton(SpriteBatch b, Rectangle bounds, string label, bool enabled)
    {
        DrawPanel(b, bounds, enabled ? new Color(229, 143, 57) : new Color(126, 91, 57), new Color(91, 56, 29));
        Vector2 labelSize = Game1.smallFont.MeasureString(label);
        b.DrawString(
            Game1.smallFont,
            label,
            new Vector2(bounds.Center.X - labelSize.X / 2, bounds.Center.Y - labelSize.Y / 2),
            enabled ? Game1.textColor : new Color(70, 57, 45)
        );
    }

    private static void DrawPanel(SpriteBatch b, Rectangle bounds, Color fill, Color border)
    {
        bool largePanel = bounds.Width > 100 || bounds.Height > 100;
        int borderSize = largePanel ? 8 : 4;
        int innerBorderSize = largePanel ? 4 : 2;
        if (largePanel)
        {
            b.Draw(Game1.staminaRect, new Rectangle(bounds.X + 6, bounds.Y + 6, bounds.Width, bounds.Height), Color.Black * 0.22f);
        }

        b.Draw(Game1.staminaRect, bounds, border);
        b.Draw(
            Game1.staminaRect,
            new Rectangle(bounds.X + borderSize / 2, bounds.Y + borderSize / 2, bounds.Width - borderSize, bounds.Height - borderSize),
            new Color(199, 86, 25)
        );
        b.Draw(
            Game1.staminaRect,
            new Rectangle(bounds.X + borderSize, bounds.Y + borderSize, bounds.Width - borderSize * 2, bounds.Height - borderSize * 2),
            fill
        );
        if (largePanel)
        {
            b.Draw(
                Game1.staminaRect,
                new Rectangle(bounds.X + borderSize + innerBorderSize, bounds.Y + borderSize + innerBorderSize, bounds.Width - (borderSize + innerBorderSize) * 2, 4),
                new Color(255, 255, 255) * 0.12f
            );
        }
    }
}
