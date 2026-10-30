#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../config"

if [[ ! -f .env ]]; then
  echo "support/dev/config/.env missing - run ./support/dev/setup.sh first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

: "${EMBERONE_S3_ACCESS_KEY:?EMBERONE_S3_ACCESS_KEY not set in support/dev/config/.env}"
: "${EMBERONE_S3_SECRET_KEY:?EMBERONE_S3_SECRET_KEY not set in support/dev/config/.env}"
EMBERONE_S3_IDENTITY_NAME="${EMBERONE_S3_IDENTITY_NAME:-emberone-local}"

content=$(cat seaweedfs-s3.json.template)
content=${content//\$\{EMBERONE_S3_ACCESS_KEY\}/$EMBERONE_S3_ACCESS_KEY}
content=${content//\$\{EMBERONE_S3_SECRET_KEY\}/$EMBERONE_S3_SECRET_KEY}
content=${content//\$\{EMBERONE_S3_IDENTITY_NAME\}/$EMBERONE_S3_IDENTITY_NAME}
printf '%s\n' "$content" > seaweedfs-s3.json
