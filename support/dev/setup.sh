#!/usr/bin/env bash
set -euo pipefail

DEV_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS_DIR="$DEV_DIR/scripts"
ROOT="$(git -C "$DEV_DIR" rev-parse --show-toplevel)"
# shellcheck source=scripts/lib/env-from-example.sh
source "$SCRIPTS_DIR/lib/env-from-example.sh"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--prompt] [--intellij]

Creates support/dev/config/.env, backend/.env and frontend/.env and installs the Git hooks.
Missing secrets are generated (shared between Docker and backend). Existing values are kept;
delete a file to regenerate it.

  --prompt    Enter secrets manually instead of generating them.
  --intellij  Print backend/.env as one line for an IntelliJ run configuration.
EOF
}

run_all() {
  prompt_shared_secrets_once "$DEV_DIR/config/.env" "$ROOT/backend/.env"
  export_shared_setup_prefills
  export EMBERONE_DEV_SETUP_INTERNAL=1

  "$SCRIPTS_DIR/setup-docker-env.sh"
  echo
  "$SCRIPTS_DIR/setup-backend.sh"
  echo
  "$SCRIPTS_DIR/setup-frontend.sh"
  echo
  "$ROOT/support/scripts/install-git-hooks.sh"

  echo
  echo "Setup done. Next: ./support/dev/scripts/start-docker.sh"
  echo "Login: akadmin, password = AUTHENTIK_BOOTSTRAP_PASSWORD in support/dev/config/.env"
}

for arg in "$@"; do
  case "$arg" in
    --prompt ) export EMBERONE_DEV_SETUP_PROMPT=1 ;;
    --intellij ) export EMBERONE_DEV_SETUP_INTELLIJ=1 ;;
    -h|--help|help ) usage; exit 0 ;;
    * )
      echo "Unknown option: $arg" >&2
      usage >&2
      exit 1
      ;;
  esac
done

run_all
