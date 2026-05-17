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
    }

    public sealed class Farmer
    {
        public bool addItemToInventoryBool(Item item)
        {
            return true;
        }
    }

    public abstract class Item
    {
    }

    public static class ItemRegistry
    {
        public static Item Create(string itemId)
        {
            return null!;
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
}
