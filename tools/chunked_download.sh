#!/bin/sh
# Downloads a URL in bounded-size byte-range chunks and concatenates them.
# Works around an environment where long-lived HTTP connections get reset
# partway through large single-shot downloads, but short requests complete fine.
set -e
URL="$1"
OUT="$2"
TOTAL="$3"
CHUNK="${4:-10485760}" # 10MB default

: > "$OUT"
START=0
while [ "$START" -lt "$TOTAL" ]; do
  END=$((START + CHUNK - 1))
  if [ "$END" -ge "$TOTAL" ]; then
    END=$((TOTAL - 1))
  fi
  PART="$OUT.part"
  ATTEMPT=0
  until curl -sS -m 180 --retry 3 --retry-delay 2 -r "${START}-${END}" -o "$PART" "$URL"; do
    ATTEMPT=$((ATTEMPT + 1))
    if [ "$ATTEMPT" -ge 5 ]; then
      echo "FAILED chunk ${START}-${END} after ${ATTEMPT} attempts" >&2
      exit 1
    fi
    echo "retrying chunk ${START}-${END} (attempt ${ATTEMPT})" >&2
    sleep 2
  done
  cat "$PART" >> "$OUT"
  rm -f "$PART"
  DONE=$((END + 1))
  echo "progress: ${DONE} / ${TOTAL} bytes ($((DONE * 100 / TOTAL))%)"
  START=$((END + 1))
done
echo "done: $OUT ($(stat -c%s "$OUT") bytes)"
