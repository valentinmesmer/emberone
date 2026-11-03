#!/usr/bin/env bash
# Shared helpers for setup scripts (bash 3.2 compatible, e.g. macOS default bash).

read_existing_env_value() {
  local file=$1
  local want_key=$2
  local line k v

  [[ -f "$file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" =~ ^[[:space:]]*# ]] || [[ -z "${line// /}" ]]; then
      continue
    fi
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      k="${BASH_REMATCH[1]}"
      v="${BASH_REMATCH[2]}"
      v="${v#\"}"
      v="${v%\"}"
      if [[ "$k" == "$want_key" ]]; then
        printf '%s' "$v"
        return 0
      fi
    fi
  done < "$file"
}

setup_prefill_var() {
  local key=$1
  printf '__SETUP_PREFILL_%s' "$key"
}

setup_prefill_value() {
  local key=$1
  local var
  var="$(setup_prefill_var "$key")"
  printf '%s' "${!var:-}"
}

set_setup_prefill() {
  local key=$1
  local value=$2
  local var
  var="$(setup_prefill_var "$key")"
  printf -v "$var" '%s' "$value"
}

# Dev secrets are generated unless EMBERONE_DEV_SETUP_PROMPT=1 (setup.sh --prompt).
setup_prompt_enabled() {
  [[ "${EMBERONE_DEV_SETUP_PROMPT:-}" == "1" ]]
}

# Hex only: safe for `source .env`, compose --env-file and URLs.
generate_dev_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 24
  else
    LC_ALL=C tr -dc 'a-f0-9' < /dev/urandom | head -c 48
  fi
}

# Generated value, or prompted value in --prompt mode. Result in REPLY.
new_secret_value() {
  local label=$1
  if setup_prompt_enabled; then
    prompt_env_value "$label" ""
  else
    REPLY="$(generate_dev_secret)"
  fi
}

# Keys that must match between dev/config/.env and backend/.env (prompt once in full setup).
# Format: "prompt label|KEY1,KEY2,..."
SHARED_SECRET_SPECS=(
  "postgres owner password|EMBERONE_POSTGRES_PASSWORD"
  "postgres migrator password|EMBERONE_POSTGRES_MIGRATOR_PASSWORD,FLYWAY_PASSWORD"
  "postgres app password|EMBERONE_POSTGRES_APP_PASSWORD,DATASOURCE_PASSWORD"
  "Authentik API token|AUTHENTIK_API_TOKEN"
  "S3 secret key|EMBERONE_S3_SECRET_KEY,OBJECT_STORAGE_SECRET_KEY"
  "Redis password|EMBERONE_REDIS_PASSWORD,REDIS_PASSWORD"
)

shared_secret_keys_into_array() {
  local keys_csv=$1
  SHARED_SECRET_KEY_LIST=()
  IFS=',' read -r -a SHARED_SECRET_KEY_LIST <<< "$keys_csv"
}

read_shared_secret_existing() {
  local keys_csv=$1
  local file=$2
  local key existing

  shared_secret_keys_into_array "$keys_csv"
  for key in "${SHARED_SECRET_KEY_LIST[@]}"; do
    existing="$(read_existing_env_value "$file" "$key")"
    if [[ -n "$existing" ]]; then
      printf '%s' "$existing"
      return 0
    fi
  done
  return 0
}

set_shared_secret_prefill() {
  local keys_csv=$1
  local value=$2
  local key

  shared_secret_keys_into_array "$keys_csv"
  for key in "${SHARED_SECRET_KEY_LIST[@]}"; do
    set_setup_prefill "$key" "$value"
  done
}

# Sub-setup scripts run in separate processes; export prefills after prompt_shared_secrets_once.
export_shared_setup_prefills() {
  local spec label keys_csv key var

  for spec in "${SHARED_SECRET_SPECS[@]}"; do
    IFS='|' read -r label keys_csv <<< "$spec"
    shared_secret_keys_into_array "$keys_csv"
    for key in "${SHARED_SECRET_KEY_LIST[@]}"; do
      var="$(setup_prefill_var "$key")"
      if [[ -n "${!var:-}" ]]; then
        export "$var"
      fi
    done
  done
}

# Fill __SETUP_PREFILL_* for shared secrets from dev/config/.env (backend-only setup).
apply_shared_secret_prefill_from_docker_env() {
  local docker_env=$1
  local spec label keys_csv value

  [[ -f "$docker_env" ]] || return 0

  for spec in "${SHARED_SECRET_SPECS[@]}"; do
    IFS='|' read -r label keys_csv <<< "$spec"
    value="$(read_shared_secret_existing "$keys_csv" "$docker_env")"
    if [[ -n "$value" ]]; then
      set_shared_secret_prefill "$keys_csv" "$value"
    fi
  done
}

# Prompt once per shared secret group; used at the start of ./support/dev/setup.sh (full setup only).
prompt_shared_secrets_once() {
  local dev_env=$1
  local backend_env=$2
  local spec label keys_csv value existing showed_intro=0

  if setup_prompt_enabled && [[ ! -r /dev/tty ]]; then
    echo "--prompt requires an interactive terminal." >&2
    return 1
  fi

  for spec in "${SHARED_SECRET_SPECS[@]}"; do
    IFS='|' read -r label keys_csv <<< "$spec"
    existing=""
    if [[ -f "$dev_env" ]]; then
      existing="$(read_shared_secret_existing "$keys_csv" "$dev_env")"
    fi
    if [[ -z "$existing" && -f "$backend_env" ]]; then
      existing="$(read_shared_secret_existing "$keys_csv" "$backend_env")"
    fi

    if [[ -n "$existing" ]]; then
      value=$existing
    else
      if setup_prompt_enabled && [[ $showed_intro -eq 0 ]]; then
        echo
        echo "Shared secrets (same values for Docker and backend):"
        showed_intro=1
      fi
      new_secret_value "$label"
      value=$REPLY
    fi
    set_shared_secret_prefill "$keys_csv" "$value"
  done
}

prompt_env_value() {
  local key=$1
  local existing=${2:-}

  REPLY=""
  if [[ -n "$existing" ]]; then
    echo
    echo "${key} [Enter to keep the current value]:"
    IFS= read -rs REPLY < /dev/tty
    if [[ -z "$REPLY" ]]; then
      REPLY=$existing
      return 0
    fi
    return 0
  fi

  while [[ -z "$REPLY" ]]; do
    echo
    echo "${key}:"
    IFS= read -rs REPLY < /dev/tty
    if [[ -z "$REPLY" ]]; then
      echo "  Required. Enter a value or Ctrl+C to abort." >&2
    fi
  done
}

key_in_list() {
  local needle=$1
  shift
  local k
  for k in "$@"; do
    [[ "$k" == "$needle" ]] && return 0
  done
  return 1
}

# Write output env file from a KEY=VALUE example file. Per key: existing value in the output
# file > shared prefill > example default > generated secret (prompted with --prompt).
# Keys only present in an existing output file are kept.
# Args: example_path output_path
build_env_from_example() {
  local example_path=$1
  local output_path=$2
  local generated=0

  if [[ ! -f "$example_path" ]]; then
    echo "Example file not found: $example_path" >&2
    return 1
  fi

  if setup_prompt_enabled && [[ ! -r /dev/tty ]]; then
    echo "--prompt requires an interactive terminal." >&2
    return 1
  fi

  local keys_order=()
  local values_order=()
  local line key value existing prefill i

  exec 3< "$example_path"
  while IFS= read -r line <&3 || [[ -n "$line" ]]; do
    if [[ "$line" =~ ^[[:space:]]*# ]] || [[ -z "${line// /}" ]]; then
      continue
    fi
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      value="${value#\"}"
      value="${value%\"}"
      existing="$(read_existing_env_value "$output_path" "$key")"
      prefill="$(setup_prefill_value "$key")"
      if [[ -n "$existing" ]]; then
        value=$existing
      elif [[ -z "$value" && -n "$prefill" ]]; then
        value=$prefill
      elif [[ -z "$value" ]]; then
        new_secret_value "$key"
        value=$REPLY
        generated=$((generated + 1))
      fi
      keys_order+=("$key")
      values_order+=("$value")
    fi
  done
  exec 3<&-

  local extra_lines=()
  if [[ -f "$output_path" ]]; then
    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)= ]] && ! key_in_list "${BASH_REMATCH[1]}" "${keys_order[@]}"; then
        extra_lines+=("$line")
      fi
    done < "$output_path"
  fi

  {
    i=0
    while [[ $i -lt ${#keys_order[@]} ]]; do
      printf '%s=%s\n' "${keys_order[$i]}" "${values_order[$i]}"
      i=$((i + 1))
    done
    for line in ${extra_lines[@]+"${extra_lines[@]}"}; do
      printf '%s\n' "$line"
    done
  } > "$output_path"

  if [[ $generated -gt 0 ]]; then
    echo "Wrote ${output_path} (${generated} new secret(s))."
  else
    echo "Wrote ${output_path}."
  fi
}
