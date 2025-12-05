#!/usr/bin/env bash

PID_TO_WAIT="$1"
TGZ="$2"
DEST="$3"

# Wait for the given PID (if any) to exit, silently
if [[ -n "$PID_TO_WAIT" ]]; then
  while kill -0 "$PID_TO_WAIT" 2>/dev/null; do
    sleep 1
  done
fi

# Delete old version
rm -rf "$DEST"

mkdir -p "$DEST"
tar --strip-components=1 -xzf "$TGZ" -C "$DEST"

# Rerun AIJ
$DEST/bin/AstroImageJ