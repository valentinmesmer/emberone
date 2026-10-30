#!/usr/bin/env bash
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/dev-paths.sh
source "$SCRIPTS_DIR/lib/dev-paths.sh"

dev_paths_init

echo "Stopping containers and removing volumes..."
dev_compose down -v --remove-orphans

echo "Regenerating SeaweedFS S3 config..."
"$SCRIPTS_DIR/render-seaweedfs-s3.sh"

echo "Starting containers..."
dev_compose up -d
"$SCRIPTS_DIR/bootstrap-db-roles.sh"

echo "Done."
