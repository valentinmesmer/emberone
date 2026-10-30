#!/usr/bin/env bash
# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Provisions the database roles. Idempotent; runs on every start (start-docker.sh, reset-docker.sh).
#
# - emberone_migrator owns the database and the public schema and runs Flyway. No superuser, no BYPASSRLS.
# - emberone_app is the runtime role. No DDL. Its grants are applied by Flyway (afterMigrate.sql).
#
# The bootstrap superuser (EMBERONE_POSTGRES_USER) only runs this script. The app never uses it.
set -euo pipefail

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/dev-paths.sh
source "$SCRIPTS_DIR/lib/dev-paths.sh"
dev_paths_init

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

BOOTSTRAP_USER="${EMBERONE_POSTGRES_USER:-emberone}"
DB="${EMBERONE_POSTGRES_DB:-emberone}"
export MIGRATOR_ROLE="${EMBERONE_POSTGRES_MIGRATOR_USER:-emberone_migrator}"
export MIGRATOR_PASSWORD="${EMBERONE_POSTGRES_MIGRATOR_PASSWORD:?EMBERONE_POSTGRES_MIGRATOR_PASSWORD missing in $ENV_FILE}"
export APP_ROLE="${EMBERONE_POSTGRES_APP_USER:-emberone_app}"
export APP_PASSWORD="${EMBERONE_POSTGRES_APP_PASSWORD:?EMBERONE_POSTGRES_APP_PASSWORD missing in $ENV_FILE}"

for _ in $(seq 1 60); do
  if dev_compose exec -T postgres pg_isready -U "$BOOTSTRAP_USER" -d "$DB" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

dev_compose exec -T \
  -e MIGRATOR_ROLE -e MIGRATOR_PASSWORD -e APP_ROLE -e APP_PASSWORD \
  postgres psql -U "$BOOTSTRAP_USER" -d "$DB" -v ON_ERROR_STOP=1 -q <<'SQL'
\set migrator `printenv MIGRATOR_ROLE`
\set migrator_password `printenv MIGRATOR_PASSWORD`
\set app `printenv APP_ROLE`
\set app_password `printenv APP_PASSWORD`

SELECT format('CREATE ROLE %I', :'migrator')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'migrator') \gexec
SELECT format('ALTER ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L',
  :'migrator', :'migrator_password') \gexec

SELECT format('CREATE ROLE %I', :'app')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app') \gexec
SELECT format('ALTER ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS PASSWORD %L',
  :'app', :'app_password') \gexec

-- Ownership moves from the bootstrap superuser to the migrator. Objects created before this
-- change are handed over (public schema only; system objects stay with the bootstrap user).
-- On a fresh database this is a no-op.
SELECT format('ALTER DATABASE %I OWNER TO %I', current_database(), :'migrator') \gexec
ALTER SCHEMA public OWNER TO :"migrator";
SET emberone.migrator TO :'migrator';
DO $$
DECLARE
  r record;
  target text := current_setting('emberone.migrator');
  bootstrap oid := (SELECT oid FROM pg_roles WHERE rolname = current_user);
BEGIN
  FOR r IN
    SELECT c.oid::regclass AS obj, c.relkind
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relowner = bootstrap AND c.relkind IN ('r', 'p', 'S', 'v', 'm')
      -- Sequences of identity/serial columns follow their table; only standalone sequences are moved here.
      AND NOT (c.relkind = 'S' AND EXISTS (
        SELECT 1 FROM pg_depend d
        WHERE d.objid = c.oid AND d.classid = 'pg_class'::regclass AND d.refclassid = 'pg_class'::regclass
          AND d.deptype IN ('a', 'i')))
  LOOP
    EXECUTE format('ALTER %s %s OWNER TO %I',
      CASE r.relkind WHEN 'S' THEN 'SEQUENCE' WHEN 'v' THEN 'VIEW' WHEN 'm' THEN 'MATERIALIZED VIEW' ELSE 'TABLE' END,
      r.obj, target);
  END LOOP;
  FOR r IN
    SELECT p.oid::regprocedure AS obj
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proowner = bootstrap
  LOOP
    EXECUTE format('ALTER ROUTINE %s OWNER TO %I', r.obj, target);
  END LOOP;
END
$$;

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app') \gexec
SQL

echo "Database roles ready: migrator=${MIGRATOR_ROLE}, app=${APP_ROLE}."
