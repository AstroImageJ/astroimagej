#!/usr/bin/env bash

# Get log file
# Use XDG_STATE_HOME if available, else ~/.local/state
xdg_state="${XDG_STATE_HOME:-}"
if [[ -n "$xdg_state" ]]; then
  state_base="$xdg_state"
else
  home="${HOME:-.}"
  state_base="$home/.local/state"
fi

# Convention: ~/.local/state/astroimagej/logs/elevator.log
elevator_log="$state_base/astroimagej/logs/elevator.log"
rm -f "$elevator_log"
exec >>"$elevator_log" 2>&1

echo "Starting AIJ elevator"

PID_TO_WAIT="$1"
TGZ="$2"
DEST="$3"

# Wait for the given PID (if any) to exit, silently
if [[ -n "$PID_TO_WAIT" ]]; then
  TIMEOUT=4
  ELAPSED=0
  while kill -0 "$PID_TO_WAIT" 2>/dev/null; do
    echo "Waiting for AIJ to close..."
    if [[ $ELAPSED -ge $TIMEOUT ]]; then
      break
    fi
    sleep 1
    ((ELAPSED++))
  done
fi

# Delete old version
echo "Removing old installation..."
rm -rf "$DEST"

echo "Creating install folder..."
mkdir -p "$DEST"
echo "Unpacking AIJ..."
tar --strip-components=1 -xzf "$TGZ" -C "$DEST"

if [[ -f "$DEST/AstroImageJ" ]]; then
  echo "Creating symlink..."
  ln -s "$DEST/bin/AstroImageJ" "$DEST"
fi

# Rerun AIJ
echo "Relaunching AIJ..."
$DEST/bin/AstroImageJ