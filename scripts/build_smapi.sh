#!/usr/bin/env bash
set -euo pipefail

common_root="${1:?Usage: scripts/build_smapi.sh <common-checkout-root>}"
common_project="${common_root}/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj"
common_assets="${common_root}/stardewvalley/assets"

dotnet build --configuration Release \
  -p:CommonProjectPath="${common_project}" \
  -p:CommonAssetsPath="${common_assets}"

common_version="$(grep -m1 '<Version>' "${common_project}" | sed -E 's#.*<Version>([^<]+)</Version>.*#\1#')"
mod_version="$(grep -m1 '<Version>' DissolverEnhanced.StardewValley.Smapi.csproj | sed -E 's#.*<Version>([^<]+)</Version>.*#\1#')"
game_version="$(grep -m1 '<GameVersion>' DissolverEnhanced.StardewValley.Smapi.csproj | sed -E 's#.*<GameVersion>([^<]+)</GameVersion>.*#\1#')"
archive_name="dissolver-enhanced-${mod_version}-smapi-sv${game_version}-c${common_version}"

python3 scripts/package_smapi.py --build-dir build --archive "${archive_name}.zip"
dotnet run --project tests/SmapiTextureSmoke/SmapiTextureSmoke.csproj --configuration Release -- "${archive_name}.zip"
