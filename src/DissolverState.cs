namespace DissolverEnhanced.StardewValley.Smapi;

internal static class DissolverState
{
    private static readonly object SyncRoot = new();
    private static readonly SortedSet<string> learnedItems = new(StringComparer.OrdinalIgnoreCase);
    private static string stateFile = "";
    private static long storedEmc;
    private static bool dirty;

    public static long StoredEmc
    {
        get
        {
            lock (SyncRoot)
            {
                return storedEmc;
            }
        }
    }

    public static int LearnedCount
    {
        get
        {
            lock (SyncRoot)
            {
                return learnedItems.Count;
            }
        }
    }

    public static IReadOnlyList<string> LearnedItems
    {
        get
        {
            lock (SyncRoot)
            {
                return learnedItems.ToArray();
            }
        }
    }

    public static string StateFile
    {
        get
        {
            lock (SyncRoot)
            {
                return stateFile;
            }
        }
    }

    public static void Load(string configDirectory, string saveKey, string playerKey, bool privateEmc)
    {
        lock (SyncRoot)
        {
            string saveDirectory = Path.Combine(configDirectory, "saves", SanitizeFileName(saveKey));
            MigrateStorageMode(saveDirectory, SanitizeFileName(playerKey), privateEmc);

            stateFile = privateEmc
                ? Path.Combine(saveDirectory, "players", SanitizeFileName(playerKey), "dissolver-state.properties")
                : Path.Combine(saveDirectory, "dissolver-state.properties");
            storedEmc = 0;
            learnedItems.Clear();
            dirty = false;

            if (!File.Exists(stateFile))
            {
                return;
            }

            foreach (string rawLine in File.ReadAllLines(stateFile))
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

                if (parts[0].Equals("stored_emc", StringComparison.OrdinalIgnoreCase) && long.TryParse(parts[1], out long loadedEmc))
                {
                    storedEmc = Math.Max(0, loadedEmc);
                }
                else if (parts[0].Equals("learned", StringComparison.OrdinalIgnoreCase))
                {
                    foreach (string itemId in parts[1].Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                    {
                        learnedItems.Add(itemId);
                    }
                }
            }
        }
    }

    public static void Save()
    {
        lock (SyncRoot)
        {
            if (dirty)
            {
                SaveLocked();
                dirty = false;
            }
        }
    }

    public static void AddEmc(long amount)
    {
        if (amount <= 0)
        {
            return;
        }

        lock (SyncRoot)
        {
            storedEmc += amount;
            dirty = true;
        }
    }

    public static bool TrySpendEmc(long amount)
    {
        if (amount <= 0)
        {
            return true;
        }

        lock (SyncRoot)
        {
            if (storedEmc < amount)
            {
                return false;
            }

            storedEmc -= amount;
            dirty = true;
            return true;
        }
    }

    public static bool Learn(string itemId)
    {
        lock (SyncRoot)
        {
            bool added = learnedItems.Add(itemId);
            if (added)
            {
                dirty = true;
            }

            return added;
        }
    }

    public static bool Unlearn(string itemId)
    {
        lock (SyncRoot)
        {
            bool removed = learnedItems.Remove(itemId);
            if (removed)
            {
                dirty = true;
            }

            return removed;
        }
    }

    public static bool IsLearned(string itemId)
    {
        lock (SyncRoot)
        {
            return learnedItems.Contains(itemId);
        }
    }

    private static void SaveLocked()
    {
        if (string.IsNullOrWhiteSpace(stateFile))
        {
            return;
        }

        Directory.CreateDirectory(Path.GetDirectoryName(stateFile)!);
        File.WriteAllLines(stateFile, new[]
        {
            "# Stored Dissolver state for Stardew Valley.",
            "stored_emc=" + storedEmc,
            "learned=" + string.Join(",", learnedItems)
        });
    }

    private static void MigrateStorageMode(string saveDirectory, string currentPlayerKey, bool privateEmc)
    {
        string modeFile = Path.Combine(saveDirectory, "dissolver-state-mode.properties");
        bool? previousPrivateEmc = ReadPreviousMode(modeFile);
        if (previousPrivateEmc == privateEmc)
        {
            return;
        }

        string sharedFile = Path.Combine(saveDirectory, "dissolver-state.properties");
        string playersDirectory = Path.Combine(saveDirectory, "players");
        if (previousPrivateEmc == false && privateEmc && File.Exists(sharedFile))
        {
            SplitSharedState(sharedFile, playersDirectory, currentPlayerKey);
        }
        else if (previousPrivateEmc == true && !privateEmc)
        {
            CombinePrivateStates(sharedFile, playersDirectory);
        }
        else if (previousPrivateEmc == null && privateEmc && File.Exists(sharedFile))
        {
            SplitSharedState(sharedFile, playersDirectory, currentPlayerKey);
        }
        else if (previousPrivateEmc == null && !privateEmc && Directory.Exists(playersDirectory))
        {
            CombinePrivateStates(sharedFile, playersDirectory);
        }

        Directory.CreateDirectory(saveDirectory);
        File.WriteAllText(modeFile, "private_emc=" + privateEmc.ToString().ToLowerInvariant() + Environment.NewLine);
    }

    private static bool? ReadPreviousMode(string modeFile)
    {
        if (!File.Exists(modeFile))
        {
            return null;
        }

        foreach (string rawLine in File.ReadAllLines(modeFile))
        {
            string line = rawLine.Trim();
            if (!line.StartsWith("private_emc=", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            return line.Split('=', 2)[1].Equals("true", StringComparison.OrdinalIgnoreCase);
        }

        return null;
    }

    private static void SplitSharedState(string sharedFile, string playersDirectory, string currentPlayerKey)
    {
        StateSnapshot shared = ReadSnapshot(sharedFile);
        SortedSet<string> players = new(StringComparer.OrdinalIgnoreCase) { currentPlayerKey };
        if (Directory.Exists(playersDirectory))
        {
            foreach (string playerDirectory in Directory.GetDirectories(playersDirectory))
            {
                players.Add(Path.GetFileName(playerDirectory));
            }
        }

        long baseShare = players.Count == 0 ? shared.StoredEmc : shared.StoredEmc / players.Count;
        long remainder = players.Count == 0 ? 0 : shared.StoredEmc % players.Count;
        int index = 0;
        foreach (string player in players)
        {
            long playerEmc = baseShare + (index < remainder ? 1 : 0);
            WriteSnapshot(Path.Combine(playersDirectory, player, "dissolver-state.properties"), new StateSnapshot(playerEmc, shared.LearnedItems));
            index++;
        }
    }

    private static void CombinePrivateStates(string sharedFile, string playersDirectory)
    {
        long combinedEmc = 0;
        SortedSet<string> combinedLearned = new(StringComparer.OrdinalIgnoreCase);
        if (Directory.Exists(playersDirectory))
        {
            foreach (string playerFile in Directory.GetFiles(playersDirectory, "dissolver-state.properties", SearchOption.AllDirectories))
            {
                StateSnapshot player = ReadSnapshot(playerFile);
                combinedEmc += player.StoredEmc;
                foreach (string itemId in player.LearnedItems)
                {
                    combinedLearned.Add(itemId);
                }
            }
        }

        WriteSnapshot(sharedFile, new StateSnapshot(combinedEmc, combinedLearned));
    }

    private static StateSnapshot ReadSnapshot(string file)
    {
        long emc = 0;
        SortedSet<string> learned = new(StringComparer.OrdinalIgnoreCase);
        if (!File.Exists(file))
        {
            return new StateSnapshot(emc, learned);
        }

        foreach (string rawLine in File.ReadAllLines(file))
        {
            string line = rawLine.Trim();
            string[] parts = line.Split('=', 2);
            if (parts.Length != 2)
            {
                continue;
            }

            if (parts[0].Equals("stored_emc", StringComparison.OrdinalIgnoreCase) && long.TryParse(parts[1], out long loadedEmc))
            {
                emc = Math.Max(0, loadedEmc);
            }
            else if (parts[0].Equals("learned", StringComparison.OrdinalIgnoreCase))
            {
                foreach (string itemId in parts[1].Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                {
                    learned.Add(itemId);
                }
            }
        }

        return new StateSnapshot(emc, learned);
    }

    private static void WriteSnapshot(string file, StateSnapshot snapshot)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(file)!);
        File.WriteAllLines(file, new[]
        {
            "# Stored Dissolver state for Stardew Valley.",
            "stored_emc=" + snapshot.StoredEmc,
            "learned=" + string.Join(",", snapshot.LearnedItems)
        });
    }

    private static string SanitizeFileName(string value)
    {
        char[] invalid = Path.GetInvalidFileNameChars();
        string sanitized = new(value.Select(character => invalid.Contains(character) ? '_' : character).ToArray());
        return string.IsNullOrWhiteSpace(sanitized) ? "unknown-save" : sanitized;
    }

    private sealed record StateSnapshot(long StoredEmc, IReadOnlyCollection<string> LearnedItems);
}
