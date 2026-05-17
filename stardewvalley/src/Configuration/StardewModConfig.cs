namespace DissolverEnhanced.StardewValley.Common.Configuration;

public sealed class StardewModConfig
{
    private const string ConfigFileName = "dissolver_enhanced.properties";
    private const string DefaultEmcFileName = "default-emc-values.yaml";
    private const string EmcOverridesFileName = "emc-overrides.yaml";
    private const string AnalyticsNotice = "This only tracks things that are needed to keep the mod going. "
        + "As you are able to turn these off, we ask that you don't, as it gives us the motivation to keep going :)";

    public const string DefaultAnalyticsEndpoint = "https://us.i.posthog.com/capture/";
    public const string DefaultAnalyticsErrorEndpoint = "https://us.i.posthog.com/i/v0/e/";
    public const string DefaultAnalyticsProjectToken = "phc_y6HcjBmwz7UEPQhaqYmJhdtHbh6VzRrv9h2MthHoLkdY";

    private StardewModConfig(string configDirectory, IReadOnlyDictionary<string, string> values)
    {
        ConfigDirectory = configDirectory;
        ConfigFile = Path.Combine(configDirectory, ConfigFileName);
        DefaultEmcValuesFile = Path.Combine(configDirectory, DefaultEmcFileName);
        EmcOverridesFile = Path.Combine(configDirectory, EmcOverridesFileName);
        EmcOnHud = ReadBool(values, "emc_on_hud", false);
        PrivateEmc = ReadBool(values, "private_emc", false);
        CreativeItems = ReadBool(values, "creative_items", false);
        Difficulty = ReadString(values, "difficulty", "hard");
        Mode = ReadString(values, "mode", "default");
        AnalyticsEnabled = ReadBool(values, "analytics_enabled", true);
        AnalyticsTester = ReadBool(values, "analytics_tester", false)
            || ReadBool(values, "tester", false)
            || ReadBool(values, "Tester", false);
        AnalyticsEndpoint = DefaultAnalyticsEndpoint;
        AnalyticsErrorEndpoint = DefaultAnalyticsErrorEndpoint;
        AnalyticsProjectToken = DefaultAnalyticsProjectToken;
    }

    public string ConfigDirectory { get; }
    public string ConfigFile { get; }
    public string DefaultEmcValuesFile { get; }
    public string EmcOverridesFile { get; }
    public bool EmcOnHud { get; }
    public bool PrivateEmc { get; }
    public bool CreativeItems { get; }
    public string Difficulty { get; }
    public string Mode { get; }
    public bool AnalyticsEnabled { get; }
    public bool AnalyticsTester { get; }
    public string AnalyticsEndpoint { get; }
    public string AnalyticsErrorEndpoint { get; }
    public string AnalyticsProjectToken { get; }

    public static StardewModConfig Load(string modDirectory)
    {
        string configDirectory = Path.Combine(modDirectory, "config", "dissolver-enhanced");
        Directory.CreateDirectory(configDirectory);

        string configFile = Path.Combine(configDirectory, ConfigFileName);
        EnsureConfigFile(configFile);
        EnsureDefaultEmcFile(Path.Combine(configDirectory, DefaultEmcFileName));
        EnsureOverrideTemplate(Path.Combine(configDirectory, EmcOverridesFileName));

        return new StardewModConfig(configDirectory, ReadProperties(configFile));
    }

    private static void EnsureConfigFile(string configFile)
    {
        if (File.Exists(configFile))
        {
            RemoveInternalAnalyticsConfigEntries(configFile);
            EnsureAnalyticsConfigEntries(configFile);
            return;
        }

        File.WriteAllText(configFile, DefaultConfigText());
    }

    private static void EnsureAnalyticsConfigEntries(string configFile)
    {
        string existing = File.Exists(configFile) ? File.ReadAllText(configFile) : "";
        List<string> missing = DefaultConfigEntries()
            .Where(entry => entry.Key.StartsWith("analytics_", StringComparison.Ordinal)
                && !ContainsProperty(existing, entry.Key))
            .Select(ConfigLine)
            .ToList();

        if (missing.Count == 0)
        {
            return;
        }

        using StreamWriter writer = File.AppendText(configFile);
        if (existing.Length > 0 && !existing.EndsWith('\n'))
        {
            writer.WriteLine();
        }
        writer.WriteLine("# " + AnalyticsNotice);
        foreach (string line in missing)
        {
            writer.WriteLine(line);
        }
    }

    private static void RemoveInternalAnalyticsConfigEntries(string configFile)
    {
        string[] keptLines = File.ReadAllLines(configFile)
            .Where(line => !IsInternalAnalyticsConfigLine(line))
            .ToArray();
        File.WriteAllLines(configFile, keptLines);
    }

    private static bool IsInternalAnalyticsConfigLine(string line)
    {
        string trimmed = line.Trim();
        return trimmed.StartsWith("analytics_endpoint=", StringComparison.Ordinal)
            || trimmed.StartsWith("analytics_error_endpoint=", StringComparison.Ordinal)
            || trimmed.StartsWith("analytics_project_token=", StringComparison.Ordinal);
    }

    private static void EnsureDefaultEmcFile(string file)
    {
        File.WriteAllText(file, "schema: 1\nitems: {}\ntags: {}\n");
    }

    private static void EnsureOverrideTemplate(string file)
    {
        if (File.Exists(file))
        {
            return;
        }

        File.WriteAllText(file, "schema: 1\nitems: {}\ntags: {}\n");
    }

    private static string DefaultConfigText()
    {
        return string.Join(Environment.NewLine, DefaultConfigEntries().Select(ConfigLine)) + Environment.NewLine;
    }

    private static IReadOnlyList<ConfigEntry> DefaultConfigEntries()
    {
        return new[]
        {
            new ConfigEntry("emc_on_hud", "false", "Display current EMC on the HUD."),
            new ConfigEntry("private_emc", "false", "Give each player their own EMC storage."),
            new ConfigEntry("creative_items", "false", "Allow creative-only items to have EMC."),
            new ConfigEntry("difficulty", "hard", "Recipe difficulty preset."),
            new ConfigEntry("mode", "default", "Balance mode preset."),
            new ConfigEntry("analytics_enabled", "true", AnalyticsNotice)
        };
    }

    private static string ConfigLine(ConfigEntry entry)
    {
        return $"{entry.Key}={entry.DefaultValue} # {entry.Comment} [default: {entry.DefaultValue}]";
    }

    private static bool ContainsProperty(string content, string key)
    {
        return content.Split('\n')
            .Select(line => line.Trim())
            .Any(line => line.StartsWith(key + "=", StringComparison.Ordinal));
    }

    private static IReadOnlyDictionary<string, string> ReadProperties(string configFile)
    {
        Dictionary<string, string> values = new(StringComparer.OrdinalIgnoreCase);
        foreach (string rawLine in File.ReadAllLines(configFile))
        {
            string line = rawLine.Trim();
            if (line.Length == 0 || line.StartsWith('#'))
            {
                continue;
            }

            string[] parts = line.Split('=', 2);
            if (parts.Length != 2)
            {
                continue;
            }

            values[parts[0].Trim()] = parts[1].Split(" #", 2)[0].Trim();
        }

        return values;
    }

    private static bool ReadBool(IReadOnlyDictionary<string, string> values, string key, bool defaultValue)
    {
        return values.TryGetValue(key, out string? rawValue)
            ? rawValue.Equals("true", StringComparison.OrdinalIgnoreCase)
            : defaultValue;
    }

    private static string ReadString(IReadOnlyDictionary<string, string> values, string key, string defaultValue)
    {
        return values.TryGetValue(key, out string? rawValue) ? rawValue : defaultValue;
    }

    private sealed record ConfigEntry(string Key, string DefaultValue, string Comment);
}
