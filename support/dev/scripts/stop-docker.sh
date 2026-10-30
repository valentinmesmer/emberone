#!/usr/bin/env bash
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/dev-paths.sh
source "$SCRIPTS_DIR/lib/dev-paths.sh"

dev_paths_init

echo "Stopping containers..."
dev_compose down --remove-orphans

echo "Done."
