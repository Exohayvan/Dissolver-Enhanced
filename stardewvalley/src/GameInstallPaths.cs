namespace DissolverEnhanced.StardewValley.Common;

public sealed record GameInstallPaths(
    string GameDirectory,
    string ModsDirectory,
    string StardewValleyAssembly,
    string SmapiAssembly
)
{
    public static GameInstallPaths FromGameDirectory(string gameDirectory)
    {
        string modsDirectory = Path.Combine(gameDirectory, "Mods");
        return new GameInstallPaths(
            gameDirectory,
            modsDirectory,
            Path.Combine(gameDirectory, "Stardew Valley.dll"),
            Path.Combine(gameDirectory, "StardewModdingAPI.dll")
        );
    }
}
