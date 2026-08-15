#!/usr/bin/env bash
# Capture Play Store screenshots off a real phone, one prompt per shot.
#
#   bash tools/capture_screenshots.sh
#
# Writes store/screenshots/NN-name.png. Play wants at least 4 phone screenshots; the shot list
# below is the listing order from docs/13-STORE-LISTING.md §3.
#
# Take these from versionCode 4 or later — the 2026-08-15 layout round changed every screen
# worth showing, and a tablet capture will not represent the phone layout.

set -u

ADB="${ADB:-/c/Android/sdk/platform-tools/adb.exe}"
OUT="store/screenshots"

if [ ! -x "$ADB" ] && ! command -v "$ADB" >/dev/null 2>&1; then
  echo "adb not found at $ADB — set ADB=/path/to/adb and re-run." >&2
  exit 1
fi

devices=$("$ADB" devices | sed -n '2,$p' | grep -c "device$" || true)
if [ "$devices" -eq 0 ]; then
  echo "No device attached. Plug the phone in, accept the USB-debugging prompt, re-run." >&2
  exit 1
fi
if [ "$devices" -gt 1 ]; then
  echo "More than one device attached. Unplug the tablet — these must be phone screenshots." >&2
  "$ADB" devices >&2
  exit 1
fi

mkdir -p "$OUT"

echo "Device: $("$ADB" shell getprop ro.product.model | tr -d '\r')"
echo "Build:  $("$ADB" shell dumpsys package com.mikhilnaika.continueapp | grep -m1 versionName | tr -d '\r ')"
echo

shoot() {
  local file="$1" desc="$2"
  echo "──────────────────────────────────────────────────────────"
  echo "  $desc"
  read -r -p "  Set the screen up, then press Enter (or 's' to skip): " answer
  case "$answer" in
    s|S) echo "  skipped"; return ;;
  esac
  "$ADB" exec-out screencap -p > "$OUT/$file"
  local bytes
  bytes=$(wc -c < "$OUT/$file")
  if [ "$bytes" -lt 10000 ]; then
    echo "  ⚠ $file is only $bytes bytes — capture probably failed, retry this one."
  else
    echo "  ✓ $OUT/$file ($((bytes / 1024)) KB)"
  fi
}

shoot "01-draw.png"        "DRAW, mid-deal: dials set, cards fanned out of the dispenser."
shoot "02-pile.png"        "THE PILE: a full grid of covers, time-budget bar visible."
shoot "03-credits.png"     "CREDITS ROLL, mid-scroll, key art behind the stats."
shoot "04-rank.png"        "RANK: two games head to head."
shoot "05-paywall.png"     "GO PRO paywall (debug build shows populated prices)."
shoot "06-share.png"       "OPTIONAL — the share sheet sitting over YouTube."

echo
echo "Done. Screenshots in $OUT/"
echo "Play requires: PNG/JPEG, each side 320–3840px, long side ≤ 2× the short side."
echo "A raw phone screenshot satisfies all of that — do not upscale or add device frames."
