# Dissolver Enhanced

![CurseForge Downloads](https://img.shields.io/curseforge/dt/1533227?style=for-the-badge)

Dissolver Enhanced is a standalone EMC-style transmutation mod inspired by ProjectE and Equivalent Exchange. Learn items, dissolve extras into EMC, then spend that stored value to recreate items you have already unlocked.

The mod is built around keeping the core experience simple: collect resources, teach them to the Dissolver, and turn unwanted items into something useful later.

## Features

- Dissolver block for learning items, storing EMC, and recreating learned items.
- Optional global or per-player EMC storage.
- EMC values from defaults, tags, recipes, and server overrides.
- EMC tooltips so item values are easy to check.
- Crystal Frame item for crafting and remote Dissolver access.
- WTHIT support where available.
- Work-in-progress Condenser and Materializer machines for more advanced EMC workflows.

## How It Works

Every supported item has an EMC value. Put an item into the Dissolver to learn it, then use stored EMC to create that item again later.

For example, if dirt is worth `1` EMC and a diamond is worth `4200` EMC, you can dissolve enough items to build up `4200` EMC, then spend it to create a diamond once the diamond has been learned.

## Config

Config files are created in:

```text
config/dissolver-enhanced/
```

The main config controls gameplay options like HUD display, private EMC, creative item values, recipe difficulty, and balance mode. Pack makers can also use EMC override files to tune item and tag values without editing the mod.

## Commands

Admin and utility commands are included for managing player EMC, learned items, and debug reports. Command availability may vary slightly by version and loader.

Common commands include:

- `/emc`
- `/emc give`
- `/emc take`
- `/emc set`
- `/emcmemory add`
- `/emcmemory remove`
- `/opendissolver`

## Supported Versions

Supported Minecraft versions and launchers are listed on the wiki:

https://github.com/Exohayvan/Dissolver-Enhanced/wiki/Supported-Versions-Launchers

## Future Plans

Dissolver Enhanced currently focuses on Minecraft, but the project is being structured so shared transmutation-style logic can eventually support more games and modding platforms. Stardew Valley support is being explored as an early step in that direction.

For Minecraft, planned improvements include better modded item values, more recipe and loot sources, stronger pack-maker tools, and continued work on EMC machines.

## Screenshots

![Dissolver screenshot](https://i.imgur.com/H9Ug8rE.png)
![Dissolver screenshot](https://i.imgur.com/277hqs7.png)

## Crafting

Crafting recipes can be found on the wiki:

https://github.com/Exohayvan/Dissolver-Enhanced/wiki/Crafting

## Modpacks

You may include Dissolver Enhanced in modpacks. For beta or alpha builds, test the exact version first before adding it to a pack intended for regular play.

## Credits

Dissolver Enhanced is based on [Dissolver / VanillaEMC](https://github.com/vassbo/Dissolver) by vassbo.

Inspired by the Transmutation Table from ProjectE / Equivalent Exchange.

## License

Dissolver Enhanced is licensed under the MIT license. See [LICENSE](LICENSE) for details.
