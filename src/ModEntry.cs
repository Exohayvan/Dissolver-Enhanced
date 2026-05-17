using DissolverEnhanced.StardewValley.Common;
using DissolverEnhanced.StardewValley.Common.Analytics;
using DissolverEnhanced.StardewValley.Common.Configuration;
using StardewModdingAPI;

namespace DissolverEnhanced.StardewValley.Smapi;

public sealed class ModEntry : Mod
{
    private StardewModAnalytics? analytics;

    public override void Entry(IModHelper helper)
    {
        StardewModConfig config = StardewModConfig.Load(helper.DirectoryPath);
        analytics = new StardewModAnalytics(
            config,
            ModVersion(),
            GameCompatibility.TargetGameVersion,
            GameCompatibility.TargetSmapiVersion,
            "client",
            LogAnalyticsWarning
        );
        analytics.Start();

        AppDomain.CurrentDomain.UnhandledException += OnUnhandledException;
        TaskScheduler.UnobservedTaskException += OnUnobservedTaskException;
        AppDomain.CurrentDomain.ProcessExit += OnProcessExit;

        Monitor.Log(
            $"Loaded Dissolver Enhanced for Stardew Valley {GameCompatibility.TargetGameVersion} with SMAPI {GameCompatibility.TargetSmapiVersion}.",
            LogLevel.Info
        );
    }

    private void OnUnhandledException(object sender, UnhandledExceptionEventArgs args)
    {
        if (args.ExceptionObject is Exception exception)
        {
            analytics?.CaptureException(exception, handled: false);
        }
    }

    private void OnUnobservedTaskException(object? sender, UnobservedTaskExceptionEventArgs args)
    {
        analytics?.CaptureException(args.Exception, handled: false);
    }

    private void OnProcessExit(object? sender, EventArgs args)
    {
        analytics?.Dispose();
    }

    private void LogAnalyticsWarning(string message, Exception? exception)
    {
        string logMessage = exception == null ? message : message + " " + exception;
        Monitor.Log(logMessage, LogLevel.Warn);
    }

    private static string ModVersion()
    {
        return typeof(ModEntry).Assembly.GetName().Version?.ToString() ?? "unknown";
    }
}
