#!/usr/bin/env bash
# Build the hierarchy-first Dissolver Enhanced architecture maps.
# Outputs stay on this data/graphify-out worktree so source worktrees remain clean.
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
workspace_root="$(cd -- "$script_dir/../.." && pwd)"

case "${1:-build}" in
  build)
    exec python3 "$script_dir/build_architecture_map.py" --root "$workspace_root" --out "$script_dir"
    ;;
  2d)
    exec open "$script_dir/graph-2d.html"
    ;;
  3d)
    exec open "$script_dir/graph-3d.html"
    ;;
  *)
    printf '%s\n' 'Usage: graphify-run.sh [build|2d|3d]' >&2
    exit 64
    ;;
esac
