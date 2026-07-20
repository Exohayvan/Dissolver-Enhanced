using System.Collections;
using System.IO;
using System.Reflection;
using DissolverEnhanced.StardewValley.Common;
using DissolverEnhanced.StardewValley.Common.Analytics;
using DissolverEnhanced.StardewValley.Common.Configuration;
using DissolverEnhanced.StardewValley.Common.Content;
using StardewModdingAPI;

namespace DissolverEnhanced.StardewValley.Smapi;

public sealed class ModEntry : Mod
{
    private StardewModAnalytics? analytics;
    private StardewModConfig? config;
    private object? gameContentHelper;
    private object? inputHelper;
    private bool loggedContentRegistration;
    private bool dumpedEmcOverridesThisSession;

    public override void Entry(IModHelper helper)
    {
        config = StardewModConfig.Load(helper.DirectoryPath);
        EmcValueRegistry.Reload(config.DefaultEmcValuesFile, config.EmcOverridesFile);
        gameContentHelper = helper.GetType().GetProperty("GameContent")?.GetValue(helper);
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

        RegisterConsoleCommand(helper);
        RegisterContentEvents(helper);
        RegisterInputEvents(helper);
        RegisterGameLoopEvents(helper);

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

    private void RegisterConsoleCommand(IModHelper helper)
    {
        object? consoleCommands = helper.GetType().GetProperty("ConsoleCommands")?.GetValue(helper);
        if (consoleCommands == null)
        {
            Monitor.Log("SMAPI console command helper was not available; de_give_dissolver was not registered.", LogLevel.Warn);
            return;
        }

        MethodInfo? add = consoleCommands.GetType().GetMethod("Add");
        Action<string, string[]> giveCallback = GiveDissolverCommand;
        add?.Invoke(
            consoleCommands,
            new object[]
            {
                "de_give_dissolver",
                "Adds a Dissolver big craftable to your inventory for sprite/UI testing.",
                giveCallback
            }
        );

        Action<string, string[]> uiCallback = OpenDissolverUiCommand;
        add?.Invoke(
            consoleCommands,
            new object[]
            {
                "de_open_dissolver_ui",
                "Opens the temporary Dissolver chest-style UI for testing.",
                uiCallback
            }
        );
    }

    private void RegisterInputEvents(IModHelper helper)
    {
        inputHelper = helper.GetType().GetProperty("Input")?.GetValue(helper);
        object? events = helper.GetType().GetProperty("Events")?.GetValue(helper);
        object? inputEvents = events?.GetType().GetProperty("Input")?.GetValue(events);
        EventInfo? buttonPressed = inputEvents?.GetType().GetEvent("ButtonPressed");
        if (inputEvents == null || buttonPressed?.EventHandlerType == null)
        {
            Monitor.Log("SMAPI input event helper was not available; placed Dissolver clicks will not open the UI.", LogLevel.Warn);
            return;
        }

        MethodInfo? handler = GetType().GetMethod(nameof(OnButtonPressed), BindingFlags.Instance | BindingFlags.NonPublic);
        Delegate callback = Delegate.CreateDelegate(buttonPressed.EventHandlerType, this, handler!);
        buttonPressed.AddEventHandler(inputEvents, callback);
    }

    private void RegisterGameLoopEvents(IModHelper helper)
    {
        object? events = helper.GetType().GetProperty("Events")?.GetValue(helper);
        object? gameLoopEvents = events?.GetType().GetProperty("GameLoop")?.GetValue(events);
        EventInfo? saveLoaded = gameLoopEvents?.GetType().GetEvent("SaveLoaded");
        EventInfo? saving = gameLoopEvents?.GetType().GetEvent("Saving");
        EventInfo? saved = gameLoopEvents?.GetType().GetEvent("Saved");
        if (gameLoopEvents == null || saveLoaded?.EventHandlerType == null)
        {
            Monitor.Log("SMAPI game loop events were not available; default EMC value generation will not run.", LogLevel.Warn);
            return;
        }

        MethodInfo? saveLoadedHandler = GetType().GetMethod(nameof(OnSaveLoaded), BindingFlags.Instance | BindingFlags.NonPublic);
        Delegate saveLoadedCallback = Delegate.CreateDelegate(saveLoaded.EventHandlerType, this, saveLoadedHandler!);
        saveLoaded.AddEventHandler(gameLoopEvents, saveLoadedCallback);

        MethodInfo? savedHandler = GetType().GetMethod(nameof(OnSaved), BindingFlags.Instance | BindingFlags.NonPublic);
        if (saved?.EventHandlerType != null && savedHandler != null)
        {
            Delegate savedCallback = Delegate.CreateDelegate(saved.EventHandlerType, this, savedHandler);
            saved.AddEventHandler(gameLoopEvents, savedCallback);
        }
        else
        {
            MethodInfo? savingHandler = GetType().GetMethod(nameof(OnSaving), BindingFlags.Instance | BindingFlags.NonPublic);
            if (saving?.EventHandlerType != null && savingHandler != null)
            {
                Delegate savingCallback = Delegate.CreateDelegate(saving.EventHandlerType, this, savingHandler);
                saving.AddEventHandler(gameLoopEvents, savingCallback);
            }
        }
    }

    private void RegisterContentEvents(IModHelper helper)
    {
        object? events = helper.GetType().GetProperty("Events")?.GetValue(helper);
        object? contentEvents = events?.GetType().GetProperty("Content")?.GetValue(events);
        EventInfo? assetRequested = contentEvents?.GetType().GetEvent("AssetRequested");
        if (contentEvents == null || assetRequested?.EventHandlerType == null)
        {
            Monitor.Log("SMAPI content event helper was not available; Dissolver content was not registered.", LogLevel.Warn);
            return;
        }

        MethodInfo? handler = GetType().GetMethod(nameof(OnAssetRequested), BindingFlags.Instance | BindingFlags.NonPublic);
        Delegate callback = Delegate.CreateDelegate(assetRequested.EventHandlerType, this, handler!);
        assetRequested.AddEventHandler(contentEvents, callback);
        Monitor.Log($"Registered content handler on {contentEvents.GetType().FullName}.", LogLevel.Trace);
        InvalidateContentCache(helper, "Data/BigCraftables");
        InvalidateContentCache(helper, "Data/CraftingRecipes");
    }

    private void OnAssetRequested(object? sender, object args)
    {
        object? name = args.GetType().GetProperty("NameWithoutLocale")?.GetValue(args);
        string displayName = name?.ToString() ?? "";
        if (!loggedContentRegistration && (displayName.Contains("BigCraftables", StringComparison.OrdinalIgnoreCase) || displayName.Contains("CraftingRecipes", StringComparison.OrdinalIgnoreCase)))
        {
            loggedContentRegistration = true;
            Monitor.Log($"Dissolver content handler saw asset '{displayName}' ({name?.GetType().FullName}).", LogLevel.Trace);
        }

        if (AssetNameMatches(name, DissolverContent.BigCraftablesAssetName))
        {
            Monitor.Log("Registering Dissolver sprite asset.", LogLevel.Trace);
            LoadBigCraftablesTexture(args);
            return;
        }

        if (AssetNameMatches(name, "Data/BigCraftables"))
        {
            Monitor.Log("Registering Dissolver big craftable data.", LogLevel.Trace);
            EditAsset(args, nameof(EditBigCraftablesAsset));
            return;
        }

        if (AssetNameMatches(name, "Data/CraftingRecipes"))
        {
            Monitor.Log("Registering Dissolver crafting recipe.", LogLevel.Trace);
            EditAsset(args, nameof(EditCraftingRecipesAsset));
        }
    }

    private static void InvalidateContentCache(IModHelper helper, string assetName)
    {
        object? gameContent = helper.GetType().GetProperty("GameContent")?.GetValue(helper);
        gameContent?.GetType().GetMethod("InvalidateCache", new[] { typeof(string) })
            ?.Invoke(gameContent, new object[] { assetName });
    }

    private static bool AssetNameMatches(object? assetName, string expectedName)
    {
        if (assetName == null)
        {
            return false;
        }

        bool equivalent = assetName.GetType().GetMethod("IsEquivalentTo", new[] { typeof(string) })
            ?.Invoke(assetName, new object[] { expectedName }) as bool? ?? false;
        return equivalent || string.Equals(assetName.ToString(), expectedName, StringComparison.OrdinalIgnoreCase);
    }

    private void LoadBigCraftablesTexture(object args)
    {
        Type? textureType = Type.GetType("Microsoft.Xna.Framework.Graphics.Texture2D, MonoGame.Framework")
            ?? Type.GetType("Microsoft.Xna.Framework.Graphics.Texture2D, StardewModdingAPI");
        MethodInfo? load = args.GetType().GetMethods()
            .FirstOrDefault(method => method.Name == "LoadFromModFile" && method.IsGenericMethodDefinition);
        if (textureType == null || load == null)
        {
            Monitor.Log("Could not register the Dissolver sprite asset.", LogLevel.Warn);
            return;
        }

        Type priorityType = load.GetParameters()[1].ParameterType;
        object priority = Enum.Parse(priorityType, "Exclusive");
        load.MakeGenericMethod(textureType).Invoke(args, new[] { "assets/big-craftables.png", priority });
    }

    private void EditAsset(object args, string editorMethodName)
    {
        MethodInfo? edit = args.GetType().GetMethods().FirstOrDefault(method => method.Name == "Edit");
        if (edit == null)
        {
            Monitor.Log($"Could not register Dissolver content edit {editorMethodName}.", LogLevel.Warn);
            return;
        }

        ParameterInfo[] parameters = edit.GetParameters();
        MethodInfo? editor = GetType().GetMethod(editorMethodName, BindingFlags.Instance | BindingFlags.NonPublic);
        Delegate callback = Delegate.CreateDelegate(parameters[0].ParameterType, this, editor!);
        object priority = Enum.Parse(parameters[1].ParameterType, "Default");
        edit.Invoke(args, new[] { callback, priority, null });
    }

    private void EditBigCraftablesAsset(object asset)
    {
        Type? dataType = Type.GetType("StardewValley.GameData.BigCraftables.BigCraftableData, StardewValley.GameData")
            ?? Type.GetType("StardewValley.GameData.BigCraftables.BigCraftableData, StardewModdingAPI");
        if (dataType == null)
        {
            Monitor.Log("Could not find Stardew's big craftable data type.", LogLevel.Warn);
            return;
        }

        IDictionary? data = GetAssetDictionary(asset, typeof(string), dataType);
        object? craftable = Activator.CreateInstance(dataType);
        if (data == null || craftable == null)
        {
            Monitor.Log("Could not edit Data/BigCraftables for the Dissolver.", LogLevel.Warn);
            return;
        }

        SetField(dataType, craftable, "Name", DissolverContent.DissolverDisplayName);
        SetField(dataType, craftable, "DisplayName", DissolverContent.DissolverDisplayName);
        SetField(dataType, craftable, "Description", DissolverContent.DissolverDescription);
        SetField(dataType, craftable, "Price", 2500);
        SetField(dataType, craftable, "Fragility", 0);
        SetField(dataType, craftable, "CanBePlacedOutdoors", true);
        SetField(dataType, craftable, "CanBePlacedIndoors", true);
        SetField(dataType, craftable, "Texture", DissolverContent.BigCraftablesAssetName);
        SetField(dataType, craftable, "SpriteIndex", 0);
        data[DissolverContent.DissolverBigCraftableId] = craftable;
    }

    private void EditCraftingRecipesAsset(object asset)
    {
        IDictionary? data = GetAssetDictionary(asset, typeof(string), typeof(string));
        if (data == null)
        {
            Monitor.Log("Could not edit Data/CraftingRecipes for the Dissolver.", LogLevel.Warn);
            return;
        }

        data[DissolverContent.DissolverRecipeName] =
            DissolverContent.DissolverRecipeForDifficulty(config?.Difficulty ?? "hard");
    }

    private void OnSaveLoaded(object? sender, object args)
    {
        UnlockDissolverRecipeForTesting();

        if (config != null)
        {
            string saveKey = CurrentSaveKey();
            string playerKey = CurrentPlayerKey();
            DissolverState.Load(config.ConfigDirectory, saveKey, playerKey, config.PrivateEmc);
            Monitor.Log(
                $"Loaded Dissolver state for save '{saveKey}' and player '{playerKey}' from {DissolverState.StateFile}. Learned {DissolverState.LearnedCount} items.",
                LogLevel.Trace
            );
        }

        if (dumpedEmcOverridesThisSession)
        {
            return;
        }

        dumpedEmcOverridesThisSession = true;
        GenerateDefaultEmcValues();
        if (config != null)
        {
            EmcValueRegistry.Reload(config.DefaultEmcValuesFile, config.EmcOverridesFile);
            Monitor.Log($"Loaded {EmcValueRegistry.ItemCount} Stardew EMC item values from defaults and overrides.", LogLevel.Info);
        }
    }

    private void UnlockDissolverRecipeForTesting()
    {
        try
        {
            Type? gameType = Type.GetType("StardewValley.Game1, Stardew Valley");
            object? player = gameType?.GetProperty("player")?.GetValue(null);
            object? recipes = player?.GetType().GetProperty("craftingRecipes")?.GetValue(player)
                ?? player?.GetType().GetField("craftingRecipes")?.GetValue(player);
            if (recipes == null)
            {
                Monitor.Log("Could not auto-unlock the Dissolver recipe because player crafting recipes were unavailable.", LogLevel.Trace);
                return;
            }

            MethodInfo? containsKey = recipes.GetType().GetMethod("ContainsKey", new[] { typeof(string) });
            bool known = containsKey?.Invoke(recipes, new object[] { DissolverContent.DissolverRecipeName }) as bool? ?? false;
            if (known)
            {
                return;
            }

            MethodInfo? add = recipes.GetType().GetMethod("Add", new[] { typeof(string), typeof(int) });
            if (add != null)
            {
                add.Invoke(recipes, new object[] { DissolverContent.DissolverRecipeName, 0 });
                Monitor.Log("Auto-unlocked the Dissolver crafting recipe for testing.", LogLevel.Info);
            }
        }
        catch (Exception exception)
        {
            Monitor.Log("Could not auto-unlock the Dissolver crafting recipe. " + exception, LogLevel.Warn);
        }
    }

    private void OnSaving(object? sender, object args)
    {
        DissolverState.Save();
    }

    private void OnSaved(object? sender, object args)
    {
        DissolverState.Save();
    }

    private static string CurrentSaveKey()
    {
        Type? gameType = Type.GetType("StardewValley.Game1, Stardew Valley");
        object? saveId = gameType?.GetProperty("uniqueIDForThisGame", BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic)?.GetValue(null)
            ?? gameType?.GetField("uniqueIDForThisGame", BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic)?.GetValue(null);
        if (saveId != null)
        {
            return saveId.ToString() ?? "unknown-save";
        }

        return CurrentPlayerKey();
    }

    private static string CurrentPlayerKey()
    {
        Type? gameType = Type.GetType("StardewValley.Game1, Stardew Valley");
        object? player = gameType?.GetProperty("player")?.GetValue(null);
        object? uniqueId = player?.GetType().GetProperty("UniqueMultiplayerID")?.GetValue(player)
            ?? player?.GetType().GetField("UniqueMultiplayerID")?.GetValue(player);
        if (uniqueId != null)
        {
            return uniqueId.ToString() ?? "unknown-player";
        }

        object? name = player?.GetType().GetProperty("Name")?.GetValue(player)
            ?? player?.GetType().GetField("Name")?.GetValue(player);
        return name?.ToString() ?? "unknown-player";
    }

    private void GenerateDefaultEmcValues()
    {
        if (config == null)
        {
            return;
        }

        try
        {
            SortedDictionary<string, EmcDumpValue> itemValues = new(StringComparer.Ordinal);
            SortedDictionary<string, SortedSet<string>> tagItems = new(StringComparer.Ordinal);

            foreach (ItemDumpSource source in ItemDumpSources())
            {
                DumpItemsFromAsset(source, itemValues, tagItems);
            }

            using StreamWriter writer = new(config.DefaultEmcValuesFile, append: false);
            writer.WriteLine("# GENERATED ON SAVE LOAD. Edit emc-overrides.yaml for custom values.");
            writer.WriteLine("# EMC is currently sell price with a 1 EMC floor for 0g items.");
            writer.WriteLine("schema: 1");
            writer.WriteLine("items:");
            foreach ((string itemId, EmcDumpValue value) in itemValues)
            {
                writer.WriteLine($"  {YamlQuote(itemId)}:");
                writer.WriteLine($"    emc: {value.Emc}");
                writer.WriteLine($"    source: {YamlQuote(value.Source)}");
                writer.WriteLine($"    raw_price: {value.RawPrice}");
            }

            writer.WriteLine("tags:");
            foreach ((string tag, SortedSet<string> items) in tagItems)
            {
                writer.WriteLine($"  {YamlQuote(tag)}:");
                writer.WriteLine("    emc: 1");
                writer.WriteLine("    items:");
                foreach (string itemId in items)
                {
                    writer.WriteLine($"      - {YamlQuote(itemId)}");
                }
            }

            Monitor.Log($"Wrote default EMC values for {itemValues.Count} Stardew items and {tagItems.Count} tags to {config.DefaultEmcValuesFile}.", LogLevel.Info);
        }
        catch (Exception exception)
        {
            Monitor.Log("Failed to generate default Stardew EMC values. " + exception, LogLevel.Warn);
        }
    }

    private void DumpItemsFromAsset(
        ItemDumpSource source,
        IDictionary<string, EmcDumpValue> itemValues,
        IDictionary<string, SortedSet<string>> tagItems)
    {
        IDictionary? data = LoadGameDataDictionary(source.AssetName, source.DataTypeName)
            ?? LoadGameDataDictionary(source.AssetName, typeof(string));
        if (data == null)
        {
            Monitor.Log($"Could not load {source.AssetName}; skipping temporary EMC values for that source.", LogLevel.Warn);
            return;
        }

        Monitor.Log($"Loaded {data.Count} entries from {source.AssetName} for default EMC values.", LogLevel.Trace);
        foreach (DictionaryEntry entry in data)
        {
            string rawId = entry.Key?.ToString() ?? "";
            if (string.IsNullOrWhiteSpace(rawId))
            {
                continue;
            }

            string qualifiedId = source.QualifiedPrefix + rawId;
            itemValues[qualifiedId] = TemporaryEmcValue(qualifiedId, entry.Value, source.PreferDataPrice);

            foreach (string tag in ReadContextTags(entry.Value))
            {
                if (!tagItems.TryGetValue(tag, out SortedSet<string>? taggedItems))
                {
                    taggedItems = new SortedSet<string>(StringComparer.Ordinal);
                    tagItems[tag] = taggedItems;
                }

                taggedItems.Add(qualifiedId);
            }
        }
    }

    private IDictionary? LoadGameDataDictionary(string assetName, string dataTypeName)
    {
        Type? dataType = ResolveDataType(dataTypeName);
        return dataType == null ? null : LoadGameDataDictionary(assetName, dataType);
    }

    private IDictionary? LoadGameDataDictionary(string assetName, Type dataType)
    {
        MethodInfo? load = gameContentHelper?.GetType().GetMethods()
            .FirstOrDefault(method => method.Name == "Load" && method.IsGenericMethodDefinition && method.GetParameters().Length == 1);
        if (gameContentHelper == null || load == null)
        {
            return null;
        }

        Type dictionaryType = typeof(Dictionary<,>).MakeGenericType(typeof(string), dataType);
        try
        {
            return load.MakeGenericMethod(dictionaryType).Invoke(gameContentHelper, new object[] { assetName }) as IDictionary;
        }
        catch (Exception exception)
        {
            string reason = ShortLoadFailureReason(exception);
            Monitor.Log($"Could not load {assetName} as {dictionaryType.Name}; {reason}.", LogLevel.Trace);
            return null;
        }
    }

    private static string ShortLoadFailureReason(Exception exception)
    {
        Exception current = exception;
        while (current.InnerException != null)
        {
            current = current.InnerException;
        }

        return current is FileNotFoundException ? "asset file was not found" : current.Message;
    }

    private static Type? ResolveDataType(string dataTypeName)
    {
        Type? type = Type.GetType(dataTypeName + ", StardewValley.GameData")
            ?? Type.GetType(dataTypeName + ", Stardew Valley");
        if (type != null)
        {
            return type;
        }

        foreach (Assembly assembly in AppDomain.CurrentDomain.GetAssemblies())
        {
            type = assembly.GetType(dataTypeName);
            if (type != null)
            {
                return type;
            }
        }

        return null;
    }

    private static EmcDumpValue TemporaryEmcValue(string qualifiedItemId, object? data, bool preferDataPrice)
    {
        int dataPrice = ReadPriceFromData(data);
        if (preferDataPrice && dataPrice > 0)
        {
            return new EmcDumpValue(dataPrice, DataSourceName(data), dataPrice);
        }

        int price = SellPriceFromItemRegistry(qualifiedItemId);
        if (price > 0)
        {
            return new EmcDumpValue(price, "item_registry_sell_price", price);
        }

        if (dataPrice > 0)
        {
            return new EmcDumpValue(dataPrice, DataSourceName(data), dataPrice);
        }

        return new EmcDumpValue(1, price == 0 ? "zero_sell_price_floor" : "missing_price_floor", price);
    }

    private static int SellPriceFromItemRegistry(string qualifiedItemId)
    {
        try
        {
            Type? itemRegistryType = Type.GetType("StardewValley.ItemRegistry, Stardew Valley");
            MethodInfo? createItem = itemRegistryType?.GetMethods()
                .FirstOrDefault(method =>
                    method.Name == "Create"
                    && !method.IsGenericMethod
                    && ParametersMatch(method, typeof(string), typeof(int), typeof(int), typeof(bool)));
            object? item = createItem?.Invoke(null, new object[] { qualifiedItemId, 1, 0, false });
            MethodInfo? sellPrice = item?.GetType().GetMethod("sellToStorePrice", new[] { typeof(long) });
            return sellPrice?.Invoke(item, new object[] { -1L }) as int? ?? -1;
        }
        catch
        {
            return -1;
        }
    }

    private static int ReadPriceFromData(object? data)
    {
        int memberPrice = ReadIntMember(data, "Price");
        if (memberPrice > 0)
        {
            return memberPrice;
        }

        return data is string rawData ? ReadPriceFromLegacyData(rawData) : memberPrice;
    }

    private static int ReadIntMember(object? instance, string name)
    {
        if (instance == null)
        {
            return 0;
        }

        object? value = instance.GetType().GetProperty(name)?.GetValue(instance)
            ?? instance.GetType().GetField(name)?.GetValue(instance);
        return value is int intValue ? intValue : 0;
    }

    private static int ReadPriceFromLegacyData(string rawData)
    {
        string[] fields = rawData.Split('/');
        foreach (int index in new[] { 1, 2, 4, 5 })
        {
            if (index < fields.Length && int.TryParse(fields[index], out int price) && price > 0)
            {
                return price;
            }
        }

        return 0;
    }

    private static string DataSourceName(object? data)
    {
        return data is string ? "legacy_string_price" : "data_price";
    }

    private static IEnumerable<string> ReadContextTags(object? instance)
    {
        if (instance == null)
        {
            yield break;
        }

        object? tags = instance.GetType().GetProperty("ContextTags")?.GetValue(instance)
            ?? instance.GetType().GetField("ContextTags")?.GetValue(instance);
        if (tags is not IEnumerable enumerable || tags is string)
        {
            yield break;
        }

        foreach (object? tag in enumerable)
        {
            string value = tag?.ToString() ?? "";
            if (!string.IsNullOrWhiteSpace(value))
            {
                yield return value;
            }
        }
    }

    private static IReadOnlyList<ItemDumpSource> ItemDumpSources()
    {
        return new[]
        {
            new ItemDumpSource("Data/Objects", "StardewValley.GameData.Objects.ObjectData", "(O)"),
            new ItemDumpSource("Data/BigCraftables", "StardewValley.GameData.BigCraftables.BigCraftableData", "(BC)"),
            new ItemDumpSource("Data/Boots", "StardewValley.GameData.Boots.BootsData", "(B)"),
            new ItemDumpSource("Data/Hats", "StardewValley.GameData.Hats.HatData", "(H)"),
            new ItemDumpSource("Data/Weapons", "StardewValley.GameData.Weapons.WeaponData", "(W)"),
            new ItemDumpSource("Data/Furniture", "StardewValley.GameData.Furniture.FurnitureData", "(F)", PreferDataPrice: true),
            new ItemDumpSource("Data/ClothingInformation", "StardewValley.GameData.ClothingData", "(S)")
        };
    }

    private static string YamlQuote(string value)
    {
        return "'" + value.Replace("'", "''") + "'";
    }

    private static IDictionary? GetAssetDictionary(object asset, Type keyType, Type valueType)
    {
        MethodInfo? asDictionary = asset.GetType().GetMethods()
            .FirstOrDefault(method => method.Name == "AsDictionary" && method.IsGenericMethodDefinition);
        object? dictionaryAsset = asDictionary?.MakeGenericMethod(keyType, valueType).Invoke(asset, Array.Empty<object>());
        return dictionaryAsset?.GetType().GetProperty("Data")?.GetValue(dictionaryAsset) as IDictionary;
    }

    private static void SetField(Type type, object instance, string name, object? value)
    {
        type.GetField(name)?.SetValue(instance, value);
    }

    private static bool IsWorldReady()
    {
        Type? contextType = Type.GetType("StardewModdingAPI.Context, StardewModdingAPI");
        return contextType?.GetProperty("IsWorldReady")?.GetValue(null) as bool? ?? false;
    }

    private static bool ParametersMatch(MethodInfo method, params Type[] parameterTypes)
    {
        ParameterInfo[] parameters = method.GetParameters();
        return parameters.Length == parameterTypes.Length
            && parameters.Select(parameter => parameter.ParameterType).SequenceEqual(parameterTypes);
    }

    private static object? CreateDissolverItem()
    {
        Type? itemRegistryType = Type.GetType("StardewValley.ItemRegistry, Stardew Valley");
        itemRegistryType?.GetMethod("ResetCache", Type.EmptyTypes)?.Invoke(null, Array.Empty<object>());
        MethodInfo? createItem = itemRegistryType?.GetMethods()
            .FirstOrDefault(method =>
                method.Name == "Create"
                && !method.IsGenericMethod
                && ParametersMatch(method, typeof(string), typeof(int), typeof(int), typeof(bool))
            );
        return createItem?.Invoke(
            null,
            new object[] { DissolverContent.DissolverQualifiedItemId, 1, 0, false }
        );
    }

    private static bool TryAddItemToInventory(object item)
    {
        Type? gameType = Type.GetType("StardewValley.Game1, Stardew Valley");
        object? player = gameType?.GetProperty("player")?.GetValue(null);
        MethodInfo? addItem = player?.GetType().GetMethods()
            .FirstOrDefault(method =>
                method.Name == "addItemToInventoryBool"
                && ParametersMatch(method, item.GetType().BaseType ?? item.GetType(), typeof(bool))
            )
            ?? player?.GetType().GetMethods()
                .FirstOrDefault(method => method.Name == "addItemToInventoryBool" && method.GetParameters().Length == 2);
        return addItem?.Invoke(player, new[] { item, true }) as bool? ?? false;
    }

    private void OnButtonPressed(object? sender, object args)
    {
        try
        {
            if (!IsWorldReady())
            {
                return;
            }

            bool shouldOpen = IsDissolverActionButton(args);
            if (!shouldOpen)
            {
                return;
            }

            object? cursor = args.GetType().GetProperty("Cursor")?.GetValue(args);
            object? tile = cursor?.GetType().GetProperty("GrabTile")?.GetValue(cursor);
            object? location = Type.GetType("StardewValley.Game1, Stardew Valley")
                ?.GetProperty("currentLocation")
                ?.GetValue(null);
            object? placedObject = GetPlacedObjectAt(location, tile);
            if (!IsDissolverObject(placedObject))
            {
                return;
            }

            SuppressInput(args);
            OpenDissolverMenu(placedObject);
        }
        catch (Exception exception)
        {
            Monitor.Log("Failed to open the Dissolver UI from a click. " + exception, LogLevel.Warn);
        }
    }

    private static bool IsDissolverActionButton(object args)
    {
        string button = args.GetType().GetProperty("Button")?.GetValue(args)?.ToString() ?? "";
        return button is "MouseRight" or "ControllerA" or "ControllerX" or "E";
    }

    private object? GetPlacedObjectAt(object? location, object? tile)
    {
        if (location == null || tile == null)
        {
            return null;
        }

        object? objects = location.GetType().GetField("objects")?.GetValue(location)
            ?? location.GetType().GetProperty("objects")?.GetValue(location)
            ?? location.GetType().GetProperty("Objects")?.GetValue(location);
        if (objects == null)
        {
            return null;
        }

        MethodInfo? tryGetValue = objects.GetType().GetMethods()
            .FirstOrDefault(method =>
            {
                ParameterInfo[] parameters = method.GetParameters();
                return method.Name == "TryGetValue"
                    && parameters.Length == 2
                    && parameters[0].ParameterType.IsInstanceOfType(tile)
                    && parameters[1].IsOut;
            });
        if (tryGetValue != null)
        {
            object?[] parameters = { tile, null };
            bool found = tryGetValue.Invoke(objects, parameters) as bool? ?? false;
            return found ? parameters[1] : null;
        }

        return objects is IDictionary dictionary && dictionary.Contains(tile)
            ? dictionary[tile]
            : null;
    }

    private void PickUpDissolver(object? location, object? tile)
    {
        object? objects = GetLocationObjects(location);
        if (objects == null || tile == null)
        {
            Monitor.Log("Could not pick up the Dissolver because the current location object map was unavailable.", LogLevel.Warn);
            return;
        }

        object? dissolverItem = CreateDissolverItem();
        if (dissolverItem == null)
        {
            Monitor.Log("Could not create the Dissolver item while picking it up.", LogLevel.Warn);
            return;
        }

        bool added = TryAddItemToInventory(dissolverItem);
        if (!added)
        {
            Monitor.Log("Your inventory is full, so the Dissolver was not picked up.", LogLevel.Info);
            return;
        }

        RemovePlacedObject(objects, tile);
        Monitor.Log("Picked up the Dissolver.", LogLevel.Trace);
    }

    private static object? GetLocationObjects(object? location)
    {
        return location?.GetType().GetField("objects")?.GetValue(location)
            ?? location?.GetType().GetProperty("objects")?.GetValue(location)
            ?? location?.GetType().GetProperty("Objects")?.GetValue(location);
    }

    private static void RemovePlacedObject(object objects, object tile)
    {
        MethodInfo? remove = objects.GetType().GetMethods()
            .FirstOrDefault(method =>
            {
                ParameterInfo[] parameters = method.GetParameters();
                return method.Name == "Remove"
                    && parameters.Length == 1
                    && parameters[0].ParameterType.IsInstanceOfType(tile);
            });
        if (remove != null)
        {
            remove.Invoke(objects, new[] { tile });
            return;
        }

        if (objects is IDictionary dictionary)
        {
            dictionary.Remove(tile);
        }
    }

    private static bool IsDissolverObject(object? placedObject)
    {
        if (placedObject == null)
        {
            return false;
        }

        foreach (string propertyName in new[] { "QualifiedItemId", "ItemId", "Name" })
        {
            string value = placedObject.GetType().GetProperty(propertyName)?.GetValue(placedObject)?.ToString()
                ?? placedObject.GetType().GetField(propertyName)?.GetValue(placedObject)?.ToString()
                ?? "";
            if (string.Equals(value, DissolverContent.DissolverQualifiedItemId, StringComparison.OrdinalIgnoreCase)
                || string.Equals(value, DissolverContent.DissolverBigCraftableId, StringComparison.OrdinalIgnoreCase)
                || string.Equals(value, DissolverContent.DissolverDisplayName, StringComparison.OrdinalIgnoreCase))
            {
                return true;
            }
        }

        return false;
    }

    private void SuppressInput(object args)
    {
        object? button = args.GetType().GetProperty("Button")?.GetValue(args);
        if (button == null)
        {
            return;
        }

        inputHelper?.GetType().GetMethod("Suppress")?.Invoke(inputHelper, new[] { button });
    }

    private void OpenDissolverUiCommand(string command, string[] args)
    {
        if (!IsWorldReady())
        {
            Monitor.Log("Load a save before using de_open_dissolver_ui.", LogLevel.Warn);
            return;
        }

        OpenDissolverMenu(null);
    }

    private void OpenDissolverMenu(object? sourceObject)
    {
        Type? gameType = Type.GetType("StardewValley.Game1, Stardew Valley");
        FieldInfo? activeMenuField = gameType?.GetField("activeClickableMenu", BindingFlags.Static | BindingFlags.Public);
        PropertyInfo? activeMenuProperty = gameType?.GetProperty("activeClickableMenu", BindingFlags.Static | BindingFlags.Public);
        if (activeMenuField != null)
        {
            activeMenuField.SetValue(null, new DissolverMenu(analytics));
        }
        else
        {
            activeMenuProperty?.SetValue(null, new DissolverMenu(analytics));
        }

        if (sourceObject != null)
        {
            analytics?.CaptureBlockUse("dissolver_block");
        }

        Monitor.Log("Opened the Dissolver UI.", LogLevel.Trace);
    }

    private object? CreateItemGrabMenu(Type menuType, Type itemType, object inventory, object? sourceObject)
    {
        Type listContract = typeof(IList<>).MakeGenericType(itemType);
        ConstructorInfo? simpleConstructor = menuType.GetConstructor(new[] { listContract, typeof(object) });
        if (simpleConstructor != null)
        {
            return simpleConstructor.Invoke(new[] { inventory, sourceObject });
        }

        foreach (ConstructorInfo constructor in menuType.GetConstructors())
        {
            ParameterInfo[] parameters = constructor.GetParameters();
            if (parameters.Length == 0 || !parameters[0].ParameterType.IsInstanceOfType(inventory))
            {
                continue;
            }

            object?[] values = parameters.Select(parameter => DefaultMenuArgument(parameter, inventory, sourceObject)).ToArray();
            try
            {
                return constructor.Invoke(values);
            }
            catch (TargetInvocationException)
            {
            }
            catch (ArgumentException)
            {
            }
        }

        return null;
    }

    private static object? DefaultMenuArgument(ParameterInfo parameter, object inventory, object? sourceObject)
    {
        if (parameter.Position == 0)
        {
            return inventory;
        }

        Type type = parameter.ParameterType;
        if (type == typeof(string))
        {
            return DissolverContent.DissolverDisplayName;
        }

        if (type == typeof(bool))
        {
            return parameter.Name is "showReceivingMenu" or "canBeExitedWithKey" or "playRightClickSound" or "allowRightClick";
        }

        if (type == typeof(int))
        {
            return parameter.HasDefaultValue ? parameter.DefaultValue : 0;
        }

        if (type == typeof(object))
        {
            return sourceObject;
        }

        return parameter.HasDefaultValue ? parameter.DefaultValue : null;
    }

    private void GiveDissolverCommand(string command, string[] args)
    {
        try
        {
            Type? contextType = Type.GetType("StardewModdingAPI.Context, StardewModdingAPI");
            bool isWorldReady = contextType?.GetProperty("IsWorldReady")?.GetValue(null) as bool? ?? false;
            if (!isWorldReady)
            {
                Monitor.Log("Load a save before using de_give_dissolver.", LogLevel.Warn);
                return;
            }

            object? dissolver = CreateDissolverItem();
            if (dissolver == null)
            {
                Monitor.Log($"Could not create item {DissolverContent.DissolverQualifiedItemId}. The big craftable content is not registered yet.", LogLevel.Warn);
                return;
            }

            bool added = TryAddItemToInventory(dissolver);
            Monitor.Log(added ? "Added a Dissolver to your inventory." : "Created a Dissolver, but it could not be added to your inventory.", LogLevel.Info);
        }
        catch (Exception exception)
        {
            Monitor.Log("Failed to add the Dissolver test item. " + exception, LogLevel.Warn);
        }
    }

    private static string ModVersion()
    {
        return typeof(ModEntry).Assembly.GetName().Version?.ToString() ?? "unknown";
    }

    private sealed record ItemDumpSource(string AssetName, string DataTypeName, string QualifiedPrefix, bool PreferDataPrice = false);
    private sealed record EmcDumpValue(int Emc, string Source, int RawPrice);
}
