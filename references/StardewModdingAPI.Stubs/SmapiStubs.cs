namespace StardewModdingAPI;

public abstract class Mod
{
    public IMonitor Monitor { get; protected set; } = NullMonitor.Instance;

    public abstract void Entry(IModHelper helper);
}

public interface IModHelper
{
    string DirectoryPath { get; }
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
