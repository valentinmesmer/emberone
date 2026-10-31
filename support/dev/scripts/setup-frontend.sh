#!/usr/bin/env bash
set -euo pipefail

if [[ "${EMBERONE_DEV_SETUP_INTERNAL:-}" != "1" ]]; then
  echo "Run ./support/dev/setup.sh from the repository root (full setup only)." >&2
  exit 1
fi

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
DEV_DIR="$(cd "$SCRIPTS_DIR/.." && pwd)"
ROOT="$(git -C "$DEV_DIR" rev-parse --show-toplevel)"
# shellcheck source=lib/env-from-example.sh
source "$SCRIPTS_DIR/lib/env-from-example.sh"

cd "$ROOT/frontend"

build_env_from_example ".env.example" ".env"
