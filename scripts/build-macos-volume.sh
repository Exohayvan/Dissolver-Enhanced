#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
work_root="${TMPDIR:-/tmp}/dissolver-enhanced-build"
work_dir="$work_root/worktree"

rm -rf "$work_dir"
mkdir -p "$work_root"

rsync -a \
  --delete \
  --exclude '.git/' \
  --exclude '.gradle/' \
  --exclude 'build/' \
  --exclude 'run/' \
  --exclude '._*' \
  "$repo_root/" "$work_dir/"

(
  cd "$work_dir"
  ./gradlew "$@"
)

rm -rf "$repo_root/build/libs"
mkdir -p "$repo_root/build"
if [ -d "$work_dir/build/libs" ]; then
  cp -R "$work_dir/build/libs" "$repo_root/build/libs"
fi
