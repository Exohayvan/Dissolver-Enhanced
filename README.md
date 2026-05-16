# Dissolver Enhanced Stardew Valley SMAPI 1.6.15

SMAPI build branch for Stardew Valley `1.6.15`.

The local build defaults to the detected install path:

`/Volumes/Steam Drive/SteamLibrary/steamapps/common/Stardew Valley/Contents/MacOS`

Build with:

```sh
dotnet build
```

Override paths when needed:

```sh
dotnet build -p:StardewValleyGamePath="/path/to/Stardew Valley/Contents/MacOS" -p:CommonProjectPath="../Common/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj"
```
