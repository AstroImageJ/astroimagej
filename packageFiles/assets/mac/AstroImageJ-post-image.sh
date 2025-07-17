#!/usr/bin/env bash
# -----------------------------------------------------------
# AstroImageJ-post-image.sh (macOS)
# Uses env vars: JpAppImageDir, CustomLauncherPath
# -----------------------------------------------------------
set -euo pipefail

shopt -s nullglob
APP_DIR=( "${JpAppImageDir:-}"/*/AstroImageJ.app )
shopt -u nullglob
APP_DIR=${APP_DIR[0]:-}

if [[ -z $APP_DIR ]]; then
  echo "Not found!"
else
  echo "Found at $APP_DIR"
fi

# Read vars
#APP_DIR="${JpAppImageDir:-}"/*/AstroImageJ.app
REPLACER="${CustomLauncherPath:-}"

echo "=== Post-image hook (MacOS) started ==="
echo "JpAppImageDir=$APP_DIR"
echo "ReplacementExe=$REPLACER"

if [[ -z "$REPLACER" || ! -f "$REPLACER" ]]; then
    echo "SKIP: CustomLauncherPath is not set or does not point to a valid file"
    echo "=== Aborted due to missing or invalid REPLACER ==="
    exit 0
fi

# Validate
if [[ -z "$APP_DIR" ]]; then
    echo "ERROR: JpAppImageDir not set"
    exit 1
fi

TARGET="$APP_DIR/Contents/MacOS/AstroImageJ"
if [[ ! -f "$TARGET" ]]; then
    echo "ERROR: Original not found: $TARGET"
    exit 2
fi

# Replace
chmod +w "$TARGET" || true
rm -f "$TARGET"
echo "Deleted original: $TARGET"
cp "$REPLACER" "$TARGET"
chmod +x "$TARGET"
echo "SUCCESS: Replaced launcher at $TARGET"
echo "=== Completed ==="
