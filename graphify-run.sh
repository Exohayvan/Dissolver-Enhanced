#!/usr/bin/env bash
# Build or refresh Graphify artifacts for the checked-out repository.
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$repo_root"

if ! command -v graphify >/dev/null 2>&1; then
    printf '%s\n' 'graphify is not installed or is not on PATH.' >&2
    exit 1
fi

exec graphify . "$@"
