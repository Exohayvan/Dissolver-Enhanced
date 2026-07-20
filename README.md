# Dissolver Enhanced Stardew Valley SMAPI 1.6.15

SMAPI build branch for Stardew Valley `1.6.15`.

The local build defaults to the detected install path:

`/Volumes/Steam Drive/SteamLibrary/steamapps/common/Stardew Valley/Contents/MacOS`

In the repository's standard worktree layout, keep the Common branch checked out at
`../../common` relative to this directory, then build with:

```sh
dotnet build
```

The build fails early if the Common project or required
`assets/big-craftables.png` texture is missing. Override both paths when using a
different worktree layout:

```sh
dotnet build \
  -p:StardewValleyGamePath="/path/to/Stardew Valley/Contents/MacOS" \
  -p:CommonProjectPath="/path/to/common/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj" \
  -p:CommonAssetsPath="/path/to/common/stardewvalley/assets"
```

Create the same verified package produced by CI after a Release build:

```sh
python3 scripts/package_smapi.py \
  --build-dir build \
  --archive dissolver-enhanced-smapi.zip
```

The packager rejects incomplete builds and confirms that the ZIP contains the
runtime texture path referenced by the compiled SMAPI assembly.
