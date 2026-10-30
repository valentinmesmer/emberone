#!/usr/bin/env bash
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git -C "$SCRIPTS_DIR" rev-parse --show-toplevel)"
ENV_FILE="$ROOT/backend/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE - run ./support/dev/setup.sh first." >&2
  exit 1
fi

cd "$ROOT/backend"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a
exec ./gradlew bootRun
