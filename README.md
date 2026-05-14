# Dissolver Enhanced

![CurseForge Downloads](https://img.shields.io/curseforge/dt/1533227?style=for-the-badge)

Dissolver Enhanced is a multi-loader fork of [Dissolver / VanillaEMC](https://github.com/vassbo/Dissolver). It keeps the simple ProjectE-style loop: convert items into EMC, learn them, then spend EMC to recreate learned items.

This fork is still in active development. The current focus is shared EMC value logic, better recipe and tag handling, cleaner player data, and new machine flows that can be carried across Fabric and Forge builds.

## Versions

| Minecraft | Loader | Status | Notes |
| --- | --- | --- | --- |
| `1.21.1` | Fabric | Released, with beta builds available | Main playable build line. New features usually start here before being ported. |
| `26.1.2` | Fabric | Beta builds | Active compatibility branch for the newer Minecraft/Fabric API stack. Expect UI and mapping fixes while APIs settle. |
| `1.20.1` | Forge | Alpha builds | Early Forge port. Use for testing only until parity work is finished. |

## Project Layout

This repository is split into worktrees/branches by target platform.

| Folder | Purpose |
| --- | --- |
| `Common` | Shared EMC values, reusable machine rules, and cross-loader Java logic. |
| `Fabric-1.21.1` | Fabric build for Minecraft `1.21.1`. |
| `Fabric-26.1.2` | Fabric beta build for Minecraft `26.1.2`. |
| `Forge-1.20.1` | Forge alpha build for Minecraft `1.20.1`. |

Common is not a complete mod by itself. Loader projects include it so shared logic can stay consistent across versions.

## Current Features

- Dissolver block for learning items, storing EMC, and recreating learned items.
- Global or private EMC storage depending on config.
- Crystal Frame item for crafting and remote Dissolver access.
- EMC tooltip support.
- WTHIT integration for showing EMC and learned state where supported.
- Shared default EMC values in `Common/emc-values/defaults.yaml`.
- Shared advancement JSON in `Common/data/dissolver_enhanced/advancement`.
- Recipe and item tag based EMC calculation improvements.
- Condenser WIP machine for converting items into EMC Orbs.
- EMC Orb item that stores EMC metadata and can feed value back into the Dissolver.
- Materializer WIP machine for converting EMC input toward target output items.
- Shared machine helper logic in Common, including condenser/materializer value helpers and conversion timing helpers.

## Stability

`1.21.1` is the most stable line. The `26.1.2` branch is beta quality and may need fixes as Minecraft and Fabric internals change. The `1.20.1` Forge branch is alpha quality and may lag behind Fabric features.

Modpacks are allowed, but beta and alpha builds should be tested before being added to a pack intended for regular play.

## How It Works

Each item has an EMC value. Some values are fixed in defaults or config overrides, while others are calculated from recipes and known tags. Players add items to the Dissolver to learn them, then spend stored EMC to create learned items later.

For example, if dirt is worth `1` EMC and a diamond is worth `4200` EMC, a player needs `4200` EMC to create one diamond. The same system lets players dissolve higher-value items back into stored EMC.

## Machines

### Dissolver

The Dissolver is the main block. It learns items, stores EMC, and lets players create learned items from stored EMC. Its data can be shared globally or made private per player depending on config.

### Condenser WIP

The Condenser converts input items into EMC Orbs. Conversion time is based on the EMC value of the input item. EMC Orbs can also be put back into the Condenser to combine their stored values.

### Materializer WIP

The Materializer is the reverse-style machine. It accepts an item target, EMC input items, and an upgrade core slot reserved for later. Input value builds up until enough EMC is stored to output the target item.

## Commands

Inherited commands from Dissolver are expected to remain available unless intentionally changed:

- `/emc`: get player EMC
- `/emc list`: list all players and their EMC
- `/emc give {amount} ({player})`: give EMC to a player
- `/emc take {amount} ({player})`: take EMC from a player
- `/emc set {amount} ({player})`: set EMC for a player
- `/emcmemory fill`: store all items in the Dissolver
- `/emcmemory clear`: forget all stored Dissolver items
- `/emcmemory add {item}`: store a specific item
- `/emcmemory remove {item}`: remove a specific stored item
- `/opendissolver`: open the Dissolver screen if a block is within range

Planned admin command improvements:

| Command | Purpose |
| --- | --- |
| `/emc value {amount}` | Set the EMC value for the held or selected item. |
| `/emc value get` | Show the EMC value for the held or selected item. |
| `/emc value clear` | Remove a custom EMC override and let the mod calculate the value again. |
| `/emc reload` | Reload EMC config and custom values without restarting the server. |

Debug commands are also available for pack makers and development builds. They require operator/admin permission and may write report files into the server's `debug` folder.

| Command | Purpose |
| --- | --- |
| `/emc debug` | Show a global EMC coverage summary, including total items, items with EMC, items without EMC, top missing tags, and recipe bottlenecks. |
| `/emc debug item` | Debug the item currently held by the command source, including its EMC key, value source, tags, and recipe diagnostics. |
| `/emc debug recipe` | Write detailed recipe diagnostics for the held item. |
| `/emc debug namespace` | Show EMC coverage grouped by namespace/mod id. |
| `/emc debug namespace {namespace}` | Show EMC coverage and missing-value diagnostics for one namespace/mod id. |

## Config

Runtime config is stored under `config/dissolver-enhanced/`.

| File | Purpose |
| --- | --- |
| `dissolver_enhanced.properties` | Main config toggles only. EMC value overrides do not belong here. |
| `default-emc-values.yaml` | Generated from the bundled Common EMC defaults. This file may be overwritten by mod updates. |
| `emc-overrides.yaml` | Pack/server value tuning. These values are applied after defaults and should be used for custom overrides. |

Current config options:

- `emc_on_hud=false|true`: display current EMC on the HUD
- `private_emc=false|true`: give each player their own EMC storage
- `creative_items=false|true`: allow creative-only items to have EMC
- `difficulty=easy|normal|hard`: change the Dissolver crafting recipe
- `mode=default|skyblock`: change some EMC values for different play styles

EMC YAML files support `items:` and `tags:` sections. Use `emc-overrides.yaml` for custom values, for example:

```yaml
schema: 1
items:
  minecraft:dirt: 1
tags:
  minecraft:logs: 32
```

## Advancements

Advancement data lives in Common so Fabric and Forge builds package the same advancement tree. To add one, create a JSON file under `Common/src/main/resources/data/dissolver_enhanced/advancement/`, set its `parent`, and set `display.x` / `display.y` to control the visible order in the advancement tab.

## Compatibility Goals

Dissolver Enhanced should eventually understand more than basic crafting recipes. Planned value sources include:

- Crafting, smelting, blasting, smoking, stonecutting, and other recipes
- Item and block tags
- Ore and block world generation
- Structure, chest, mob, fishing, and archaeology loot tables
- Dimension and biome restrictions
- Modded blocks and items
- Item variants with components or NBT-like data, such as enchantments, potion effects, and tipped arrows

## Roadmap

| Status | Area | Plan |
| --- | --- | --- |
| In progress | Common logic | Keep reusable EMC value and machine rules in Common for Fabric/Forge parity. |
| In progress | `26.1.2` Fabric | Stabilize UI, recipes, item registration, mixins, and player-data syncing. |
| In progress | Forge `1.20.1` | Continue alpha port and loader-specific wrappers. |
| Planned | Admin commands | Add permission-gated EMC value override and reload commands. |
| Planned | Permissions | Make admin commands respect server permissions. |
| Planned | Better mod compatibility | Improve values for modded items beyond simple crafting recipes. |
| Planned | World generation value logic | Estimate rarity from ore/block generation data. |
| Planned | Loot table value logic | Account for chest, mob, fishing, structure, and archaeology loot. |
| Planned | Variant item values | Handle enchanted books, potions, tipped arrows, and other data-driven variants. |
| Planned | Config cleanup | Make custom values and balance settings easier to edit. |
| Planned | Documentation | Keep docs current as features move between alpha, beta, and release builds. |

## Screenshots

![Dissolver screenshot](https://i.imgur.com/H9Ug8rE.png)
![Dissolver screenshot](https://i.imgur.com/277hqs7.png)

## Crafting

Crafting recipes can be found on the Wiki: https://github.com/Exohayvan/Dissolver-Enhanced/wiki/Crafting

## Credits

Dissolver Enhanced is based on [Dissolver / VanillaEMC](https://github.com/vassbo/Dissolver) by vassbo.

The mod is inspired by the Transmutation Table from ProjectE / Equivalent Exchange, with a focus on a simple standalone transmutation system for modern Minecraft.

## License

Dissolver Enhanced is licensed under the MIT license. See [LICENSE](LICENSE) for details.
