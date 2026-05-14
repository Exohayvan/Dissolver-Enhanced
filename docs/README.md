# Dissolver Enhanced EMC Converter

Static browser tool for converting ProjectE or VanillaEMC-style EMC value files into Dissolver Enhanced `emc-overrides.yaml`.

Open `index.html` directly for local use, or publish the `docs` directory with GitHub Pages.

The converter accepts:

- JSON maps such as `{ "minecraft:diamond": 4200 }`
- JSON arrays or nested objects with `item`/`id` and `emc`/`value`
- CFG/properties rows such as `S:minecraft:diamond=4200`
- VanillaEMC rows such as `emc:minecraft:dirt=50`
- Existing YAML-ish `items:` and `tags:` sections

For VanillaEMC config files, only `emc:{id}={number}` entries are converted. Settings such as `emc_on_hud`, `private_emc`, `creative_items`, `difficulty`, and `mode` are ignored because Dissolver Enhanced stores those separately.

Output is designed for `config/dissolver-enhanced/emc-overrides.yaml`.
