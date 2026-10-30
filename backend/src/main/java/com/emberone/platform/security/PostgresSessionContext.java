/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * PostgreSQL session variables used by RLS ({@code app.tenant_id}) and audit triggers ({@code
 * app.user_id}). Applied at transaction begin ({@link TenantContextJpaTransactionManager}) with
 * {@code is_local=true}.
 */
@UtilityClass
public class PostgresSessionContext {

    public static void apply(Connection connection, @Nullable Long tenantId, @Nullable Long userId)
            throws SQLException {
        if (tenantId != null && userId != null) {
            try (PreparedStatement ps =
                    connection.prepareStatement(
                            "SELECT set_config('app.tenant_id', ?, true), "
                                    + "set_config('app.user_id', ?, true)")) {
                ps.setString(1, String.valueOf(tenantId));
                ps.setString(2, String.valueOf(userId));
                ps.execute();
            }
            return;
        }
        setOrReset(connection, "app.tenant_id", tenantId);
        setOrReset(connection, "app.user_id", userId);
    }

    /**
     * Opens the read-only cross-tenant policies for this transaction ({@code app.global_admin}).
     * Never called for tenant admins or users; the GUC is transaction-local, so it needs no reset.
     */
    public static void applyGlobalAdminRead(Connection connection) throws SQLException {
        setBooleanFlag(connection, "app.global_admin", true);
    }

    public static void setObjectStoragePlatform(Connection connection, boolean enabled)
            throws SQLException {
        setBooleanFlag(connection, "app.object_storage_platform", enabled);
    }

    /**
     * When {@code true}, {@code emberone_action_log()} skips writes (scheduled retention deletes).
     */
    public static void setRetention(Connection connection, boolean enabled) throws SQLException {
        setBooleanFlag(connection, "app.retention", enabled);
    }

    private static void setBooleanFlag(Connection connection, String key, boolean enabled)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config(?, ?, true)")) {
            ps.setString(1, key);
            ps.setString(2, enabled ? "true" : "false");
            ps.execute();
        }
    }

    private static void setOrReset(Connection connection, String key, @Nullable Long value)
            throws SQLException {
        if (value != null) {
            try (PreparedStatement ps =
                    connection.prepareStatement("SELECT set_config(?, ?, true)")) {
                ps.setString(1, key);
                ps.setString(2, String.valueOf(value));
                ps.execute();
            }
        } else {
            try (PreparedStatement ps = connection.prepareStatement("RESET " + key)) {
                ps.execute();
            }
        }
    }
}
