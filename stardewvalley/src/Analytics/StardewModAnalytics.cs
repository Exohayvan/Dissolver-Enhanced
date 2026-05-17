using DissolverEnhanced.StardewValley.Common.Configuration;

namespace DissolverEnhanced.StardewValley.Common.Analytics;

public sealed class StardewModAnalytics : IDisposable
{
    private const string StartupEvent = "mod_started";
    private const string SessionStartedEvent = "session_started";
    private const string SessionEndedEvent = "session_ended";
    private const string HeartbeatEvent = "heartbeat";
    private const string BlockUsedEvent = "custom_block_used";
    private const string ConfigLoadedEvent = "config_loaded";
    private const string ItemLearnedEvent = "dissolver_item_learned";
    private const string ItemDissolvedEvent = "dissolver_item_dissolved";
    private const string ItemExtractedEvent = "dissolver_item_extracted";
    private const string ItemRejectedEvent = "dissolver_item_rejected";
    private const string AchievementEarnedEvent = "achevement_earned";
    private static readonly TimeSpan HeartbeatInterval = TimeSpan.FromMinutes(1);

    private readonly StardewModConfig config;
    private readonly string modVersion;
    private readonly string gameVersion;
    private readonly string loaderVersion;
    private readonly string runtimeSide;
    private readonly Action<string, Exception?> warningLogger;
    private readonly PostHogCaptureClient? client;
    private readonly PostHogErrorReporter? errorReporter;
    private readonly string? distinctId;
    private readonly string? sessionId;
    private Timer? heartbeatTimer;

    public StardewModAnalytics(
        StardewModConfig config,
        string modVersion,
        string gameVersion,
        string loaderVersion,
        string runtimeSide,
        Action<string, Exception?> warningLogger
    )
    {
        this.config = config;
        this.modVersion = modVersion;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.runtimeSide = runtimeSide;
        this.warningLogger = warningLogger;

        if (!config.AnalyticsEnabled || string.IsNullOrWhiteSpace(config.AnalyticsProjectToken))
        {
            return;
        }

        distinctId = LocalInstanceId.ReadOrCreate(Path.Combine(config.ConfigDirectory, "analytics-instance-id.txt"));
        sessionId = PostHogSessionId.Create();
        client = new PostHogCaptureClient(config.AnalyticsEndpoint, config.AnalyticsProjectToken, warningLogger);
        errorReporter = new PostHogErrorReporter(
            config.AnalyticsErrorEndpoint,
            config.AnalyticsProjectToken,
            distinctId,
            "DissolverEnhanced.StardewValley",
            BaseEventProperties,
            warningLogger
        );
    }

    public bool Enabled => client != null && distinctId != null;

    public void Start()
    {
        if (!Enabled)
        {
            return;
        }

        if (config.AnalyticsTester)
        {
            _ = client!.SetUserPropertiesAsync(config.AnalyticsErrorEndpoint, distinctId!, new Dictionary<string, object?>
            {
                ["is_internal"] = true
            });
        }

        _ = client!.CaptureAsync(SessionStartedEvent, distinctId!, StartupProperties());
        _ = client.CaptureAsync(StartupEvent, distinctId!, StartupProperties());
        CaptureConfigLoaded();
        heartbeatTimer = new Timer(_ => CaptureHeartbeat(), null, HeartbeatInterval, HeartbeatInterval);
    }

    public void CaptureHeartbeat(IReadOnlyDictionary<string, object?>? properties = null)
    {
        if (!Enabled)
        {
            return;
        }

        Dictionary<string, object?> eventProperties = BaseEventProperties();
        eventProperties["event_side"] = runtimeSide;
        eventProperties["world_state"] = "stardew_valley";
        eventProperties["world_info"] = "unknown";
        eventProperties["game_mode"] = "unknown";
        eventProperties["session_location"] = runtimeSide;
        if (properties != null)
        {
            foreach ((string key, object? value) in properties)
            {
                eventProperties[key] = value;
            }
        }

        _ = client!.CaptureAsync(HeartbeatEvent, distinctId!, eventProperties);
    }

    public void CaptureBlockUse(string blockId)
    {
        CaptureEvent(BlockUsedEvent, new Dictionary<string, object?>
        {
            ["block_id"] = blockId,
            ["event_side"] = runtimeSide
        });
    }

    public void CaptureConfigLoaded()
    {
        CaptureEvent(ConfigLoadedEvent, new Dictionary<string, object?>
        {
            ["event_side"] = runtimeSide,
            ["emc_on_hud"] = config.EmcOnHud,
            ["private_emc"] = config.PrivateEmc,
            ["creative_items"] = config.CreativeItems,
            ["difficulty"] = config.Difficulty,
            ["mode"] = config.Mode,
            ["emc_override_item_count"] = 0,
            ["emc_override_tag_count"] = 0
        });
    }

    public void CaptureDissolverItemLearned(string namespaceId, string item, string itemId, int stackCount, string singleValue, string totalValue, bool creativeItem)
    {
        CaptureItemEvent(ItemLearnedEvent, namespaceId, item, itemId, stackCount, singleValue, totalValue, creativeItem);
    }

    public void CaptureDissolverItemDissolved(string namespaceId, string item, string itemId, int stackCount, string singleValue, string totalValue, bool creativeItem)
    {
        CaptureItemEvent(ItemDissolvedEvent, namespaceId, item, itemId, stackCount, singleValue, totalValue, creativeItem);
    }

    public void CaptureDissolverItemExtracted(string namespaceId, string item, string itemId, int stackCount, string singleValue, string totalValue, bool creativeItem)
    {
        CaptureItemEvent(ItemExtractedEvent, namespaceId, item, itemId, stackCount, singleValue, totalValue, creativeItem);
    }

    public void CaptureDissolverItemRejected(string namespaceId, string item, string itemId, string reason)
    {
        Dictionary<string, object?> properties = ItemProperties(namespaceId, item, itemId);
        properties["reason"] = reason;
        CaptureEvent(ItemRejectedEvent, properties);
    }

    public void CaptureAchievementEarned(string achievementId, IReadOnlyDictionary<string, object?>? achievementProperties = null)
    {
        Dictionary<string, object?> properties = new()
        {
            ["event_side"] = runtimeSide,
            ["achievement_id"] = achievementId
        };
        if (achievementProperties != null)
        {
            foreach ((string key, object? value) in achievementProperties)
            {
                properties[key] = value;
            }
        }

        CaptureEvent(AchievementEarnedEvent, properties);
    }

    public void CaptureException(Exception exception, bool handled)
    {
        if (errorReporter != null)
        {
            try
            {
                errorReporter.CaptureAsync(exception, handled).GetAwaiter().GetResult();
            }
            catch (Exception reporterException)
            {
                warningLogger("Could not flush Stardew Valley exception analytics.", reporterException);
            }
        }
    }

    private void CaptureItemEvent(string eventName, string namespaceId, string item, string itemId, int stackCount, string singleValue, string totalValue, bool creativeItem)
    {
        Dictionary<string, object?> properties = ItemProperties(namespaceId, item, itemId);
        properties["stack_count"] = stackCount;
        properties["single_value"] = singleValue;
        properties["total_value"] = totalValue;
        properties["creative_item"] = creativeItem;
        CaptureEvent(eventName, properties);
    }

    private void CaptureEvent(string eventName, IReadOnlyDictionary<string, object?> properties)
    {
        if (!Enabled)
        {
            return;
        }

        Dictionary<string, object?> eventProperties = BaseEventProperties();
        foreach ((string key, object? value) in properties)
        {
            eventProperties[key] = value;
        }

        _ = client!.CaptureAsync(eventName, distinctId!, eventProperties);
    }

    private Dictionary<string, object?> StartupProperties()
    {
        Dictionary<string, object?> properties = BaseEventProperties();
        properties["event_side"] = runtimeSide;
        return properties;
    }

    private Dictionary<string, object?> BaseEventProperties()
    {
        Dictionary<string, object?> properties = new()
        {
            ["mod_id"] = "Exohayvan.DissolverEnhanced",
            ["mod_version"] = modVersion,
            ["game"] = GameCompatibility.GameId,
            ["stardew_valley_version"] = gameVersion,
            ["game_version"] = gameVersion,
            ["loader"] = GameCompatibility.LoaderId,
            ["loader_game"] = GameCompatibility.LoaderId + "-" + gameVersion,
            ["game_loadername_gameversion"] = GameCompatibility.GameId + "-" + GameCompatibility.LoaderId + "-" + gameVersion,
            ["loader_version"] = loaderVersion,
            ["runtime_side"] = runtimeSide,
            ["analytics_enabled"] = config.AnalyticsEnabled,
            ["dotnet_version"] = Environment.Version.ToString(),
            ["os_name"] = Environment.OSVersion.Platform.ToString()
        };

        if (sessionId != null)
        {
            properties["$session_id"] = sessionId;
            properties["session_id"] = sessionId;
        }

        return properties;
    }

    private Dictionary<string, object?> ItemProperties(string namespaceId, string item, string itemId)
    {
        Dictionary<string, object?> properties = new()
        {
            ["event_side"] = runtimeSide,
            ["namespace"] = namespaceId,
            ["item"] = item,
            ["item_id"] = itemId
        };
        return properties;
    }

    public void Dispose()
    {
        heartbeatTimer?.Dispose();
        if (Enabled)
        {
            Dictionary<string, object?> properties = StartupProperties();
            properties["session_location"] = runtimeSide;
            client!.CaptureAsync(SessionEndedEvent, distinctId!, properties).GetAwaiter().GetResult();
        }

        client?.Dispose();
        errorReporter?.Dispose();
    }
}
