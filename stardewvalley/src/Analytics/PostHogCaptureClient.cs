using System.Text;
using System.Text.Json;

namespace DissolverEnhanced.StardewValley.Common.Analytics;

public sealed class PostHogCaptureClient : IDisposable
{
    private static readonly TimeSpan RequestTimeout = TimeSpan.FromSeconds(5);
    private readonly HttpClient httpClient;
    private readonly string endpoint;
    private readonly string apiKey;
    private readonly Action<string, Exception?> warningLogger;

    public PostHogCaptureClient(string endpoint, string apiKey, Action<string, Exception?> warningLogger)
    {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.warningLogger = warningLogger;
        httpClient = new HttpClient
        {
            Timeout = RequestTimeout
        };
    }

    public Task CaptureAsync(string eventName, string distinctId, IReadOnlyDictionary<string, object?>? properties)
    {
        Dictionary<string, object?> eventProperties = properties == null
            ? new Dictionary<string, object?>()
            : new Dictionary<string, object?>(properties);
        eventProperties["distinct_id"] = distinctId;

        Dictionary<string, object?> body = new()
        {
            ["api_key"] = apiKey,
            ["event"] = eventName,
            ["properties"] = eventProperties,
            ["timestamp"] = DateTimeOffset.UtcNow.ToString("O")
        };

        return SendAsync(endpoint, body);
    }

    public Task SetUserPropertiesAsync(string endpoint, string distinctId, IReadOnlyDictionary<string, object?> properties)
    {
        Dictionary<string, object?> body = new()
        {
            ["api_key"] = apiKey,
            ["event"] = "$set",
            ["distinct_id"] = distinctId,
            ["properties"] = new Dictionary<string, object?> { ["$set"] = properties },
            ["timestamp"] = DateTimeOffset.UtcNow.ToString("O")
        };

        return SendAsync(endpoint, body);
    }

    private async Task SendAsync(string requestEndpoint, IReadOnlyDictionary<string, object?> body)
    {
        try
        {
            string json = JsonSerializer.Serialize(body);
            using StringContent content = new(json, Encoding.UTF8, "application/json");
            using HttpResponseMessage response = await httpClient.PostAsync(requestEndpoint, content).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                warningLogger($"PostHog capture failed with HTTP {(int)response.StatusCode}.", null);
            }
        }
        catch (Exception exception)
        {
            warningLogger("PostHog capture failed.", exception);
        }
    }

    public void Dispose()
    {
        httpClient.Dispose();
    }
}
