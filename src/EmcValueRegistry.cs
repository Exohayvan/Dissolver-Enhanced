extern alias StardewGame;

using Item = StardewGame::StardewValley.Item;

namespace DissolverEnhanced.StardewValley.Smapi;

internal static class EmcValueRegistry
{
    private static readonly object SyncRoot = new();
    private static Dictionary<string, int> values = new(StringComparer.OrdinalIgnoreCase);

    public static int ItemCount
    {
        get
        {
            lock (SyncRoot)
            {
                return values.Count;
            }
        }
    }

    public static void Reload(string defaultValuesFile, string overridesFile)
    {
        Dictionary<string, int> merged = new(StringComparer.OrdinalIgnoreCase);
        LoadItemValues(defaultValuesFile, merged, overwrite: true);
        LoadItemValues(overridesFile, merged, overwrite: true);

        lock (SyncRoot)
        {
            values = merged;
        }
    }

    public static int EmcValue(Item item)
    {
        string? qualifiedItemId = QualifiedItemId(item);
        if (!string.IsNullOrWhiteSpace(qualifiedItemId))
        {
            lock (SyncRoot)
            {
                if (values.TryGetValue(qualifiedItemId, out int value))
                {
                    return Math.Max(1, value);
                }
            }
        }

        return Math.Max(1, item.sellToStorePrice(-1));
    }

    public static int EmcValue(string qualifiedItemId)
    {
        lock (SyncRoot)
        {
            return values.TryGetValue(qualifiedItemId, out int value) ? Math.Max(1, value) : 1;
        }
    }

    public static Item? CreateItem(string qualifiedItemId, int stack = 1)
    {
        try
        {
            Type? itemRegistryType = Type.GetType("StardewValley.ItemRegistry, Stardew Valley");
            object? item = itemRegistryType?.GetMethods()
                .FirstOrDefault(method =>
                    method.Name == "Create"
                    && !method.IsGenericMethod
                    && ParametersMatch(method, typeof(string), typeof(int), typeof(int), typeof(bool)))
                ?.Invoke(null, new object[] { qualifiedItemId, Math.Max(1, stack), 0, false });
            return item as Item;
        }
        catch
        {
            return null;
        }
    }

    public static string? QualifiedItemId(Item item)
    {
        foreach (string memberName in new[] { "QualifiedItemId", "ItemId", "Name" })
        {
            object? value = item.GetType().GetProperty(memberName)?.GetValue(item)
                ?? item.GetType().GetField(memberName)?.GetValue(item);
            string? raw = value?.ToString();
            if (string.IsNullOrWhiteSpace(raw))
            {
                continue;
            }

            return raw.StartsWith('(') ? raw : "(O)" + raw;
        }

        return null;
    }

    private static void LoadItemValues(string file, IDictionary<string, int> target, bool overwrite)
    {
        if (!File.Exists(file))
        {
            return;
        }

        bool inItems = false;
        string? currentItem = null;

        foreach (string rawLine in File.ReadLines(file))
        {
            string trimmed = rawLine.Trim();
            if (trimmed.Length == 0 || trimmed.StartsWith('#'))
            {
                continue;
            }

            if (trimmed == "items:")
            {
                inItems = true;
                currentItem = null;
                continue;
            }

            if (trimmed == "tags:")
            {
                inItems = false;
                currentItem = null;
                continue;
            }

            if (!inItems)
            {
                continue;
            }

            if (rawLine.StartsWith("  ", StringComparison.Ordinal) && !rawLine.StartsWith("    ", StringComparison.Ordinal) && trimmed.EndsWith(':'))
            {
                currentItem = UnquoteYamlKey(trimmed[..^1]);
                continue;
            }

            if (currentItem != null && trimmed.StartsWith("emc:", StringComparison.Ordinal))
            {
                string rawValue = trimmed["emc:".Length..].Trim();
                if (int.TryParse(rawValue, out int value) && (overwrite || !target.ContainsKey(currentItem)))
                {
                    target[currentItem] = Math.Max(1, value);
                }
            }
        }
    }

    private static string UnquoteYamlKey(string value)
    {
        value = value.Trim();
        if (value.Length >= 2 && value[0] == '\'' && value[^1] == '\'')
        {
            return value[1..^1].Replace("''", "'");
        }

        return value;
    }

    private static bool ParametersMatch(System.Reflection.MethodInfo method, params Type[] parameterTypes)
    {
        System.Reflection.ParameterInfo[] parameters = method.GetParameters();
        if (parameters.Length != parameterTypes.Length)
        {
            return false;
        }

        for (int index = 0; index < parameterTypes.Length; index++)
        {
            if (parameters[index].ParameterType != parameterTypes[index])
            {
                return false;
            }
        }

        return true;
    }
}
