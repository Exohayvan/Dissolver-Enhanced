# Dissolver Enhanced Architecture Map

Generated deterministically from tracked text/source files in the local worktrees.

- Nodes: **1913**
- Links: **4956**
- Files: **835**
- Symbols: **869**

## Relationship counts

- `AMBIGUOUS_IMPORT`: **3**
- `CONTAINS`: **849**
- `DECLARES`: **1384**
- `EXTERNAL_IMPORT`: **1090**
- `IMPORTS`: **1298**
- `LOADER_EQUIVALENT_EXACT`: **103**
- `LOADER_EQUIVALENT_VARIANT`: **223**
- `USES_COMMON`: **6**

## Model

- **Common** is the shared hub and contains its tracked files/symbols.
- **Games** contain loaders, loaders contain version worktrees.
- `IMPORTS` only represents exact local/Common fully-qualified resolution.
- `EXTERNAL_IMPORT` represents a collapsed framework/package dependency.
- `AMBIGUOUS_IMPORT` is explicitly labelled rather than linked to arbitrary code.
- Cross-loader same-path files are connected by `LOADER_EQUIVALENT_EXACT` or `LOADER_EQUIVALENT_VARIANT`.
