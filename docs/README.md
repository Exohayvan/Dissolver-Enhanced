# Dissolver Enhanced EMC Converter

Static browser tool for converting ProjectE or VanillaEMC-style EMC value files into Dissolver Enhanced YAML.

Open `index.html` directly for local use, or publish the `web` directory with GitHub Pages.

The converter accepts:

- JSON maps such as `{ "minecraft:diamond": 4200 }`
- JSON arrays or nested objects with `item`/`id` and `emc`/`value`
- CFG/properties rows such as `S:minecraft:diamond=4200`
- Existing YAML-ish `items:` and `tags:` sections

Output is designed for `config/dissolver-enhanced/emc-overrides.yaml`.
