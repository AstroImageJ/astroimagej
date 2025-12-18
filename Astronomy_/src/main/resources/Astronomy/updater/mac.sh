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
MIGRATION="$4"

# Migration handling: if $DEST exists and this is a migration,
# user must remove app manually otherwise the installer silently fails
if [[ "$MIGRATION" == "true" ]]; then
  while [[ -d "$DEST" ]]; do
    echo "Waiting for old AIJ to be removed..."
    if ! osascript >/dev/null <<EOF
      set theDest to "$(printf '%s' "$DEST" | sed 's/"/\\"/g')"
      display dialog "
      You have selected to update AstroImageJ version 5 to version 6.

      To continue this update:
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
