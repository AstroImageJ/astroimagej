#!/usr/bin/env bash
set -euo pipefail

# Log this script to file
LOG="$HOME/Library/Logs/AstroImageJ/elevator.log"
rm -f "$LOG"
exec &> "$LOG"

echo "Starting AIJ elevator"

PID_TO_WAIT="$1"
DMG="$2"
DEST="$3"
DOWNGRADE="$4"

# Downgrade handling: if $DEST exists and this is a downgrade,
# user must remove app manually otherwise the installer silently fails
if [[ "$DOWNGRADE" == "true" ]]; then
  while [[ -d "$DEST" ]]; do
    echo "Waiting for old AIJ to be removed..."
    if ! osascript >/dev/null <<EOF
      set theDest to "$(printf '%s' "$DEST" | sed 's/"/\\"/g')"
      display dialog "
      You are attempting to downgrade AstroImageJ.
      Apple does not natively allow app downgrades.

      If you would like to continue:
      1) Keep this panel open
      2) In Finder, go to: " & theDest & "
      3) Move the AstroImageJ app to the Trash
      4) Then click OK to continue

      Otherwise, click Cancel to abort the downgrade." buttons {"Cancel", "OK"} default button "OK" cancel button "Cancel" with title "AstroImageJ Elevator"
EOF
    then
      exit 0
    fi

    # If user clicked OK but $DEST still exists, loop and show again.
  done
fi

echo "Launching installer..."
open "$DMG"
