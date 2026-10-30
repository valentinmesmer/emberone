-- Copyright (C) 2026 Valentin Mesmer
-- This file is part of EmberOne.
-- SPDX-License-Identifier: AGPL-3.0-or-later

-- Flyway callback: runs after every migrate, so tables created by any migration are covered.
-- The roles are provisioned by bootstrap-db-roles.sh; this file only grants the runtime role access.

GRANT USAGE ON SCHEMA public TO ${appRole};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${appRole};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${appRole};
