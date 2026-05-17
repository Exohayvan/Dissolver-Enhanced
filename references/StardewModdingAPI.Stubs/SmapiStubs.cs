namespace StardewModdingAPI
{
    public abstract class Mod
    {
        public IMonitor Monitor { get; protected set; } = NullMonitor.Instance;

        public abstract void Entry(IModHelper helper);
    }

    public interface IModHelper
    {
        string DirectoryPath { get; }
        IModEvents Events { get; }
        IModConsoleCommands ConsoleCommands { get; }
    }

    public interface IModEvents
    {
        IContentEvents Content { get; }
    }

    public interface IContentEvents
    {
        event EventHandler<Events.AssetRequestedEventArgs> AssetRequested;
    }

    public interface IModConsoleCommands
    {
        void Add(string name, string documentation, Action<string, string[]> callback);
    }

    public interface IMonitor
    {
        void Log(string message, LogLevel level = LogLevel.Trace);
    }

    public enum LogLevel
    {
        Trace,
        Debug,
        Info,
        Warn,
        Error,
        Alert
    }

    internal sealed class NullMonitor : IMonitor
    {
        public static readonly NullMonitor Instance = new();

        private NullMonitor()
        {
        }

        public void Log(string message, LogLevel level = LogLevel.Trace)
        {
        }
    }

    public static class Context
    {
        public static bool IsWorldReady { get; }
    }

    public enum AssetLoadPriority
    {
        Exclusive
    }
}

namespace StardewModdingAPI.Events
{
    public sealed class AssetRequestedEventArgs : EventArgs
    {
        public IAssetName NameWithoutLocale { get; } = null!;

        public void LoadFromModFile<T>(string path, StardewModdingAPI.AssetLoadPriority priority)
        {
        }

        public void Edit(Action<IAssetData> editor)
        {
        }
    }

    public interface IAssetName
    {
        bool IsEquivalentTo(string assetName);
    }

    public interface IAssetData
    {
        IAssetDataForDictionary<TKey, TValue> AsDictionary<TKey, TValue>();
    }

    public interface IAssetDataForDictionary<TKey, TValue>
    {
        IDictionary<TKey, TValue> Data { get; }
    }
}

namespace StardewValley
{
    public static class Game1
    {
        public static Farmer player { get; } = null!;
        public static Menus.IClickableMenu? activeClickableMenu { get; set; }
        public static Microsoft.Xna.Framework.Rectangle uiViewport { get; } = default;
        public static SpriteFont dialogueFont { get; } = null!;
        public static SpriteFont smallFont { get; } = null!;
        public static Microsoft.Xna.Framework.Color textColor { get; } = default;
        public static Microsoft.Xna.Framework.Graphics.Texture2D staminaRect { get; } = null!;

        public static void playSound(string cueName)
        {
        }

        public static void addHUDMessage(HUDMessage message)
        {
        }

        public static int getOldMouseX()
        {
            return 0;
        }

        public static int getOldMouseY()
        {
            return 0;
        }
    }

    public sealed class Farmer
    {
        public IList<Item?> Items { get; } = new List<Item?>();
        public Item? CursorSlotItem { get; set; }

        public bool addItemToInventoryBool(Item item)
        {
            return true;
        }

        public bool addItemToInventoryBool(Item item, bool makeActiveObject)
        {
            return true;
        }
    }

    public abstract class Item
    {
        public string DisplayName { get; } = "";
        public int Stack { get; }

        public int sellToStorePrice(long specificPlayerID)
        {
            return 0;
        }

        public void drawInMenu(Microsoft.Xna.Framework.Graphics.SpriteBatch spriteBatch, Microsoft.Xna.Framework.Vector2 location, float scaleSize)
        {
        }
    }

    public static class ItemRegistry
    {
        public static Item Create(string itemId)
        {
            return null!;
        }
    }

    public sealed class HUDMessage
    {
        public const int newQuest_type = 1;

        public HUDMessage(string message, int type)
        {
        }
    }

    public sealed class SpriteFont
    {
        public Microsoft.Xna.Framework.Vector2 MeasureString(string text)
        {
            return default;
        }
    }
}

namespace StardewValley.Menus
{
    public class IClickableMenu
    {
        protected int xPositionOnScreen;
        protected int yPositionOnScreen;
        protected int width;
        protected int height;

        public IClickableMenu()
        {
        }

        public IClickableMenu(int x, int y, int width, int height, bool showUpperRightCloseButton = false)
        {
            xPositionOnScreen = x;
            yPositionOnScreen = y;
            this.width = width;
            this.height = height;
        }

        public virtual void gameWindowSizeChanged(Microsoft.Xna.Framework.Rectangle oldBounds, Microsoft.Xna.Framework.Rectangle newBounds)
        {
        }

        public virtual void receiveLeftClick(int x, int y, bool playSound = true)
        {
        }

        public virtual void receiveRightClick(int x, int y, bool playSound = true)
        {
        }

        public virtual void performHoverAction(int x, int y)
        {
        }

        protected virtual void cleanupBeforeExit()
        {
        }

        public virtual void draw(Microsoft.Xna.Framework.Graphics.SpriteBatch b)
        {
        }

        protected static void drawHoverText(Microsoft.Xna.Framework.Graphics.SpriteBatch b, string text, StardewValley.SpriteFont font)
        {
        }

        protected static void drawMouse(Microsoft.Xna.Framework.Graphics.SpriteBatch b)
        {
        }
    }

    public sealed class ClickableComponent
    {
        public Microsoft.Xna.Framework.Rectangle bounds;
        public string name;

        public ClickableComponent(Microsoft.Xna.Framework.Rectangle bounds, string name)
        {
            this.bounds = bounds;
            this.name = name;
        }

        public bool containsPoint(int x, int y)
        {
            return bounds.Contains(x, y);
        }
    }
}

namespace StardewValley.GameData.BigCraftables
{
    public sealed class BigCraftableData
    {
        public string? Name { get; set; }
        public string? DisplayName { get; set; }
        public string? Description { get; set; }
        public int Price { get; set; }
        public bool CanBePlacedOutdoors { get; set; }
        public bool CanBePlacedIndoors { get; set; }
        public string? Texture { get; set; }
        public int SpriteIndex { get; set; }
    }
}

namespace Microsoft.Xna.Framework.Graphics
{
    public sealed class Texture2D
    {
    }

    public sealed class SpriteBatch
    {
        public void Draw(Texture2D texture, Microsoft.Xna.Framework.Rectangle destinationRectangle, Microsoft.Xna.Framework.Color color)
        {
        }

        public void DrawString(StardewValley.SpriteFont spriteFont, string text, Microsoft.Xna.Framework.Vector2 position, Microsoft.Xna.Framework.Color color)
        {
        }
    }
}

namespace Microsoft.Xna.Framework
{
    public struct Rectangle
    {
        public int X { get; set; }
        public int Y { get; set; }
        public int Width { get; set; }
        public int Height { get; set; }

        public Rectangle(int x, int y, int width, int height)
        {
            X = x;
            Y = y;
            Width = width;
            Height = height;
        }

        public Point Center => new(X + Width / 2, Y + Height / 2);

        public bool Contains(int x, int y)
        {
            return x >= X && x < X + Width && y >= Y && y < Y + Height;
        }
    }

    public struct Point
    {
        public int X { get; set; }
        public int Y { get; set; }

        public Point(int x, int y)
        {
            X = x;
            Y = y;
        }
    }

    public struct Vector2
    {
        public float X { get; set; }
        public float Y { get; set; }

        public Vector2(float x, float y)
        {
            X = x;
            Y = y;
        }
    }

    public struct Color
    {
        public static Color Black => default;

        public Color(byte r, byte g, byte b)
        {
        }

        public static Color operator *(Color color, float multiplier)
        {
            return color;
        }
    }
}
