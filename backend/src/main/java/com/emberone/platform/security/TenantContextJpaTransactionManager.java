/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;

/**
 * Sets PostgreSQL tenant/user session variables whenever a JPA transaction starts, using the
 * request-scoped values from {@link TenantContextSupport} (holder + JWT).
 */
public class TenantContextJpaTransactionManager extends JpaTransactionManager {

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        applyPostgresSessionContext();
    }

    private void applyPostgresSessionContext() {
        try {
            Connection connection = DataSourceUtils.getConnection(getDataSource());
            PostgresSessionContext.apply(
                    connection,
                    TenantContextSupport.resolveTenantId(),
                    TenantContextSupport.resolveUserId());
            if (TenantContextHolder.isGlobalAdminRead()) {
                PostgresSessionContext.applyGlobalAdminRead(connection);
            }
        } catch (SQLException ex) {
            throw new CannotCreateTransactionException(
                    "Failed to apply PostgreSQL session context", ex);
        }
    }
}
