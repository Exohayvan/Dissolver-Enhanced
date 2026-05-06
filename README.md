# Dissolver Enhanced

Dissolver Enhanced is a Fabric mod forked from [Dissolver / VanillaEMC](https://github.com/vassbo/Dissolver). The original mod adds a Dissolver block that lets players convert items into EMC, then spend that EMC to recreate stored items.

This fork is currently early in development. The goal is to keep the simple ProjectE-style transmutation feel while improving balance, compatibility, admin controls, and support for item variants that the original mod does not value well yet.

## Status

- Minecraft: `1.21`
- Mod loader: Fabric
- Required dependency: [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- Development state: work in progress
- Modpacks: allowed

## Roadmap

Use this table to track what is finished, what is being worked on, and what is still planned.

| Status | Area | Plan |
| --- | --- | --- |
| Planned | EMC value command | Add a permission-gated command for changing the EMC value of an item, such as `/emc value {amount}` while holding or targeting an item. |
| Planned | Permissions | Make admin commands respect server permissions so normal players cannot change EMC values or other protected settings. |
| Planned | Better mod compatibility | Improve how EMC values are calculated for modded items instead of relying only on simple crafting recipes. |
| Planned | World generation value logic | Estimate rarity from world generation data, including ores, block distribution, dimensions, biome restrictions, and generation frequency. |
| Planned | Loot table value logic | Use loot tables to better understand items that come from chests, mobs, fishing, structures, archaeology, or other non-crafting sources. |
| Planned | Variant item values | Add proper EMC handling for items with important data, including enchanted books, potions, tipped arrows, and similar item variants. |
| Planned | Distant Horizons compatibility | Investigate and fix the known error that happens when Dissolver is used with Distant Horizons. |
| Planned | Config cleanup | Make custom values and balance settings easier to read, edit, and maintain. |
| Planned | Documentation | Keep this README updated as features move from planned to done. |

## Planned Admin Commands

These commands are planned for Dissolver Enhanced and may change during development.

| Command | Purpose |
| --- | --- |
| `/emc value {amount}` | Set the EMC value for the item being held or selected. Requires permission. |
| `/emc value get` | Show the EMC value for the item being held or selected. |
| `/emc value clear` | Remove a custom EMC override and let the mod calculate the value again. Requires permission. |
| `/emc reload` | Reload EMC config and custom values without restarting the server. Requires permission. |

The original commands from Dissolver are still expected to be supported unless this fork changes them intentionally:

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

## Compatibility Goals

Dissolver Enhanced should eventually understand more than basic crafting recipes. The plan is to make EMC values smarter by looking at multiple sources:

- Crafting, smelting, blasting, smoking, stonecutting, and other recipes
- Ore and block world generation
- Structure, chest, mob, fishing, and archaeology loot tables
- Dimension and biome restrictions
- Modded blocks and items
- Items with components or NBT-like data, such as enchantments, potion effects, and tipped arrow effects

This should help modpacks get usable EMC values with less manual config work.

## How It Works

Each item has an EMC value. Some values may be fixed manually, while others are calculated from recipes or other data. Players add items to the Dissolver to learn them, then spend stored EMC to create learned items later.

For example, if dirt is worth `1` EMC and a diamond is worth `4200` EMC, a player would need `4200` EMC to create one diamond. The same system also lets players dissolve higher-value items back into EMC.

## The Dissolver

The Dissolver is the main block. It stores learned items and manages EMC conversion. Its stored data works globally in the world, similar to an Ender Chest, and can be shared between players or made private depending on config.

The block can be broken with a stone pickaxe or better.

## The Crystal Frame

The Crystal Frame is used to craft the Dissolver. It can also remotely access the Dissolver inventory when the player is within range of a Dissolver block.

## Screenshots

![Dissolver screenshot](https://i.imgur.com/H9Ug8rE.png)
![Dissolver screenshot](https://i.imgur.com/277hqs7.png)

## Crafting

Crystal Frame recipe:

![Crystal Frame recipe](https://i.imgur.com/6yI94wB.png)

Default Dissolver recipe:

![Dissolver recipe](https://i.imgur.com/nCiWKMZ.png)

Normal difficulty recipe:

![Normal difficulty recipe](https://i.imgur.com/525sk7u.png)

Easy difficulty recipe:

![Easy difficulty recipe](https://i.imgur.com/w8UpOF9.png)

## Config

Current inherited config options from Dissolver:

- `emc_on_hud=false|true`: display current EMC on the HUD
- `private_emc=false|true`: give each player their own EMC storage
- `creative_items=false|true`: allow creative-only items to have EMC
- `difficulty=easy|normal|hard`: change the Dissolver crafting recipe
- `mode=default|skyblock`: change some EMC values for different play styles
- `emc:{id}={number}`: set a custom EMC value for an item, for example `emc:minecraft:dirt=50`

These may be reorganized as Dissolver Enhanced adds better custom value and compatibility systems.

## Credits

Dissolver Enhanced is based on [Dissolver / VanillaEMC](https://github.com/vassbo/Dissolver) by vassbo.

The mod is inspired by the Transmutation Table from ProjectE / Equivalent Exchange, with a focus on a simple standalone transmutation system for modern Minecraft.

## License

Dissolver is licensed under the CC0-1.0 license. See [LICENSE](LICENSE) for details.
