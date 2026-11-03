#!/usr/bin/env bash
# Fail if legacy list query params appear on HTTP surfaces (greenfield ListQuery migration).
set -euo pipefail
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
PATTERN='unreadOnly|openOnly|includeArchived'
FOUND=0
while IFS= read -r line; do
  echo "$line"
  FOUND=1
done < <(
  git -C "$ROOT" grep -nE "$PATTERN" -- \
    'backend/src/main/java/**/*Controller.java' \
    'frontend/lib/api/**' || true
)
if [[ "$FOUND" -eq 1 ]]; then
  echo "Legacy list params found on API surfaces (see above)." >&2
  exit 1
fi
echo "No legacy list params on controller/API client surfaces."
