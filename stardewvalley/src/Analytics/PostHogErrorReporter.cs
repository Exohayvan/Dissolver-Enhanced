using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Diagnostics;
using System.Reflection;

namespace DissolverEnhanced.StardewValley.Common.Analytics;

public sealed class PostHogErrorReporter : IDisposable
{
    private const string ExceptionEvent = "$exception";
    private static readonly TimeSpan RequestTimeout = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan FailureLogCooldown = TimeSpan.FromMinutes(5);
    private readonly HttpClient httpClient;
    private readonly string endpoint;
    private readonly string token;
    private readonly string distinctId;
    private readonly string inAppPackagePrefix;
    private readonly Func<IReadOnlyDictionary<string, object?>> baseProperties;
    private readonly Action<string, Exception?> warningLogger;
    private DateTimeOffset nextFailureLogAt = DateTimeOffset.MinValue;

    public PostHogErrorReporter(
        string endpoint,
        string token,
        string distinctId,
        string inAppPackagePrefix,
        Func<IReadOnlyDictionary<string, object?>> baseProperties,
        Action<string, Exception?> warningLogger
    )
    {
        this.endpoint = endpoint;
        this.token = token;
        this.distinctId = distinctId;
        this.inAppPackagePrefix = inAppPackagePrefix;
        this.baseProperties = baseProperties;
        this.warningLogger = warningLogger;
        httpClient = new HttpClient
        {
            Timeout = RequestTimeout
        };
    }

    public Task CaptureAsync(Exception exception, bool handled)
    {
        if (!HasInAppFrame(exception))
        {
            return Task.CompletedTask;
        }

        Dictionary<string, object?> properties = new(baseProperties())
        {
            ["distinct_id"] = distinctId,
            ["$exception_list"] = new[] { ExceptionObject(exception, handled) },
            ["$exception_fingerprint"] = Fingerprint(exception),
            ["$exception_level"] = "error",
            ["$exception_message"] = ExceptionMessage(exception),
            ["$exception_type"] = exception.GetType().FullName,
            ["$exception_stack_trace_raw"] = exception.ToString(),
            ["exception_handled"] = handled,
            ["exception_message"] = ExceptionMessage(exception),
            ["exception_type"] = exception.GetType().FullName,
            ["exception_top_frame"] = TopFrame(exception),
            ["exception_in_app_top_frame"] = InAppTopFrame(exception),
            ["exception_stack_trace"] = exception.ToString(),
            ["exception_causes"] = ExceptionCauses(exception)
        };

        Dictionary<string, object?> body = new()
        {
            ["token"] = token,
            ["event"] = ExceptionEvent,
            ["properties"] = properties,
            ["timestamp"] = DateTimeOffset.UtcNow.ToString("O")
        };

        return SendAsync(body);
    }

    private async Task SendAsync(IReadOnlyDictionary<string, object?> body)
    {
        try
        {
            string json = JsonSerializer.Serialize(body);
            using StringContent content = new(json, Encoding.UTF8, "application/json");
            using HttpResponseMessage response = await httpClient.PostAsync(endpoint, content).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                warningLogger($"PostHog exception capture failed with HTTP {(int)response.StatusCode}.", null);
            }
        }
        catch (Exception exception)
        {
            LogFailure("PostHog exception capture failed", exception);
        }
    }

    private void LogFailure(string message, Exception exception)
    {
        DateTimeOffset now = DateTimeOffset.UtcNow;
        if (now < nextFailureLogAt)
        {
            return;
        }

        nextFailureLogAt = now.Add(FailureLogCooldown);
        warningLogger($"{message}: {exception.GetType().Name}: {exception.Message}", null);
    }

    private bool HasInAppFrame(Exception exception)
    {
        Exception? current = exception;
        while (current != null)
        {
            if ((current.StackTrace ?? "").Contains(inAppPackagePrefix, StringComparison.Ordinal))
            {
                return true;
            }

            current = current.InnerException;
        }

        return false;
    }

    private Dictionary<string, object?> ExceptionObject(Exception exception, bool handled)
    {
        return new Dictionary<string, object?>
        {
            ["type"] = exception.GetType().FullName,
            ["value"] = ExceptionMessage(exception),
            ["mechanism"] = new Dictionary<string, object?>
            {
                ["handled"] = handled,
                ["type"] = handled ? "generic" : "on_error",
                ["synthetic"] = false
            },
            ["stacktrace"] = new Dictionary<string, object?>
            {
                ["frames"] = Frames(exception)
            }
        };
    }

    private static string ExceptionMessage(Exception exception)
    {
        return string.IsNullOrWhiteSpace(exception.Message) ? exception.ToString() : exception.Message;
    }

    private static List<Dictionary<string, object?>> ExceptionCauses(Exception exception)
    {
        List<Dictionary<string, object?>> causes = new();
        Exception? current = exception.InnerException;
        while (current != null)
        {
            causes.Add(new Dictionary<string, object?>
            {
                ["type"] = current.GetType().FullName,
                ["message"] = ExceptionMessage(current)
            });
            current = current.InnerException;
        }

        return causes;
    }

    private List<Dictionary<string, object?>> Frames(Exception exception)
    {
        List<Dictionary<string, object?>> frames = new();
        foreach (StackFrame frame in new StackTrace(exception, true).GetFrames() ?? Array.Empty<StackFrame>())
        {
            MethodBase? method = frame.GetMethod();
            Type? declaringType = method?.DeclaringType;
            string module = declaringType?.FullName ?? "unknown";

            frames.Add(new Dictionary<string, object?>
            {
                ["platform"] = "custom",
                ["lang"] = "csharp",
                ["function"] = method?.Name ?? "unknown",
                ["filename"] = frame.GetFileName(),
                ["lineno"] = frame.GetFileLineNumber(),
                ["module"] = module,
                ["resolved"] = true,
                ["in_app"] = module.StartsWith(inAppPackagePrefix, StringComparison.Ordinal)
            });
        }

        return frames;
    }

    private static string TopFrame(Exception exception)
    {
        StackFrame? frame = (new StackTrace(exception, true).GetFrames() ?? Array.Empty<StackFrame>()).FirstOrDefault();
        return FrameName(frame);
    }

    private string InAppTopFrame(Exception exception)
    {
        foreach (StackFrame frame in new StackTrace(exception, true).GetFrames() ?? Array.Empty<StackFrame>())
        {
            MethodBase? method = frame.GetMethod();
            string module = method?.DeclaringType?.FullName ?? "";
            if (module.StartsWith(inAppPackagePrefix, StringComparison.Ordinal))
            {
                return FrameName(frame);
            }
        }

        return "unknown";
    }

    private static string FrameName(StackFrame? frame)
    {
        if (frame == null)
        {
            return "unknown";
        }

        MethodBase? method = frame.GetMethod();
        string module = method?.DeclaringType?.FullName ?? "unknown";
        string function = method?.Name ?? "unknown";
        int line = frame.GetFileLineNumber();
        return line > 0 ? $"{module}.{function}:{line}" : $"{module}.{function}";
    }

    private static string Fingerprint(Exception exception)
    {
        string rawFingerprint = exception.GetType().FullName + ":" + (exception.StackTrace?.Split('\n').FirstOrDefault() ?? "unknown");
        byte[] digest = SHA256.HashData(Encoding.UTF8.GetBytes(rawFingerprint));
        return Convert.ToHexString(digest).ToLowerInvariant();
    }

    public void Dispose()
    {
        httpClient.Dispose();
    }
}
