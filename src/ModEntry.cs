using DissolverEnhanced.StardewValley.Common;
using StardewModdingAPI;

namespace DissolverEnhanced.StardewValley.Smapi;

public sealed class ModEntry : Mod
{
    public override void Entry(IModHelper helper)
    {
        Monitor.Log(
            $"Loaded Dissolver Enhanced for Stardew Valley {GameCompatibility.TargetGameVersion} with SMAPI {GameCompatibility.TargetSmapiVersion}.",
            LogLevel.Info
        );
    }
}
