#!/usr/bin/env bash
# Resolve dev paths and run docker compose. Source from scripts under dev/scripts/.
# Callers outside that tree must set DEV_DIR to the repo's support/dev/ before dev_paths_init.

dev_paths_init() {
  if [[ -n "${DEV_PATHS_INITIALIZED:-}" ]]; then
    return 0
  fi
  if [[ -z "${DEV_DIR:-}" ]]; then
    local caller_dir
    caller_dir="$(cd "$(dirname "${BASH_SOURCE[1]}")" && pwd)"
    if [[ "$caller_dir" == *"/dev/scripts/lib" ]]; then
      DEV_DIR="$(cd "$caller_dir/../.." && pwd)"
    else
      DEV_DIR="$(cd "$caller_dir/.." && pwd)"
    fi
  fi
  CONFIG_DIR="$DEV_DIR/config"
  COMPOSE_FILE="$DEV_DIR/compose/docker-compose.dev.yml"
  ENV_FILE="$CONFIG_DIR/.env"
  DEV_PATHS_INITIALIZED=1
}

dev_compose() {
  dev_paths_init
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing $ENV_FILE - run ./support/dev/setup.sh first." >&2
    return 1
  fi

  if docker compose version >/dev/null 2>&1; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
    return $?
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
    return $?
  fi

  echo "Docker Compose not found. Install the Docker Compose plugin (docker compose) or docker-compose." >&2
  return 1
}

# Example command for docs / manual use (same flags as dev_compose).
dev_compose_print_cmd() {
  dev_paths_init
  if docker compose version >/dev/null 2>&1; then
    printf 'docker compose -f %q --env-file %q' "$COMPOSE_FILE" "$ENV_FILE"
  elif command -v docker-compose >/dev/null 2>&1; then
    printf 'docker-compose -f %q --env-file %q' "$COMPOSE_FILE" "$ENV_FILE"
  else
    printf 'docker compose -f %q --env-file %q' "$COMPOSE_FILE" "$ENV_FILE"
  fi
}
