# Dissolver Enhanced Graphify Architecture Maps

This worktree contains a deterministic, hierarchy-first architecture graph for the local Dissolver Enhanced workspace.

## Build

```bash
./graphify-run.sh build
```

## Open maps

```bash
./graphify-run.sh 2d
./graphify-run.sh 3d
```

## Model

```text
Dissolver Enhanced
├── Common
│   └── tracked files → declared symbols
├── Minecraft
│   └── loader → version → tracked files → declared symbols/imports
└── Stardew Valley
    └── loader → version → tracked files → declared symbols/imports
```

Each version has a `USES_COMMON` connection to the shared Common hub. The generator scans only tracked text/source/config files, excludes generated build output and caches, resolves local/common imports where possible, and writes:

- `architecture-graph.json` — portable data graph
- `graph-2d.html` — D3 force-graph map
- `graph-3d.html` — WebGL `3d-force-graph` map
- `ARCHITECTURE_MAP.md` — graph counts and model notes
