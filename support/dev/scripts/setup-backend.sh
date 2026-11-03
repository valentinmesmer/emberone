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

ENV_FILE="$ROOT/backend/.env"

apply_shared_secret_prefill_from_docker_env "$DEV_DIR/config/.env"
build_env_from_example "$ROOT/backend/.env.example" "$ENV_FILE"

if [[ "${EMBERONE_DEV_SETUP_INTELLIJ:-}" == "1" ]]; then
  line=""
  first=1
  env_line=""
  key=""
  value=""
  while IFS= read -r env_line || [[ -n "$env_line" ]]; do
    if [[ "$env_line" =~ ^[[:space:]]*# ]] || [[ -z "${env_line// /}" ]]; then
      continue
    fi
    if [[ "$env_line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      value="${value#\"}"
      value="${value%\"}"
      if [[ $first -eq 1 ]]; then
        line="${key}=${value}"
        first=0
      else
        line="${line};${key}=${value}"
      fi
    fi
  done < "$ENV_FILE"
  echo
  echo "IntelliJ → Run Configuration → Environment variables (paste as one line):"
  echo
  printf '%s\n' "$line"
  echo
fi
