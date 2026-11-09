#!/usr/bin/env bash
set -euo pipefail
RES_DIR="android/app/src/main/res"
for src in android-icons/mipmap-*; do
  [ -d "$src" ] || continue
  dir="$(basename "$src")"
  mkdir -p "$RES_DIR/$dir"
  cp -f "$src"/* "$RES_DIR/$dir/"
done
mkdir -p "$RES_DIR/values"
cp -f android-icons/values/ic_launcher_background.xml "$RES_DIR/values/ic_launcher_background.xml"
