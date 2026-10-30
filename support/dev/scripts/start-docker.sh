#!/usr/bin/env bash
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/dev-paths.sh
source "$SCRIPTS_DIR/lib/dev-paths.sh"

dev_paths_init
"$SCRIPTS_DIR/render-seaweedfs-s3.sh"
dev_compose up -d
"$SCRIPTS_DIR/bootstrap-db-roles.sh"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo
echo "Containers are up. Authentik needs 1-2 minutes on first start to apply the blueprints."
echo "  Authentik:     http://localhost:${AUTHENTIK_HTTP_PORT:-9000} (akadmin)"
echo "  Mailpit:       http://localhost:${EMBERONE_MAIL_UI_PORT:-8025}"
echo "  Redis Insight: http://localhost:${EMBERONE_REDIS_COMMANDER_PORT:-8081}"
echo "Next: ./support/dev/scripts/run-backend.sh, then cd frontend && bun install && bun dev"
