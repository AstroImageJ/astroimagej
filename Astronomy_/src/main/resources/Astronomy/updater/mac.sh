#!/usr/bin/env bash
set -euo pipefail

PID_TO_WAIT="$1"
DMG="$2"

# Wait for the given PID (if any) to exit, silently
if [[ -n "$PID_TO_WAIT" ]]; then
  while kill -0 "$PID_TO_WAIT" 2>/dev/null; do
    sleep 1
  done
fi

open "$DMG"
