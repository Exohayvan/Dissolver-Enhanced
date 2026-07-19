# Dissolver Enhanced Architecture Map

Generated deterministically from tracked text/source files in the local worktrees.

- Nodes: **1719**
- Links: **3098**
- Files: **835**
- Symbols: **869**

## Model

- **Common** is the shared hub and contains its tracked files/symbols.
- **Games** contain loaders, loaders contain version worktrees.
- Each version links to Common through `USES_COMMON`.
- Files contain declared symbols; resolved local/common imports are represented by `IMPORTS` links.
