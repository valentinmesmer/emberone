/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Detects SQL that runs outside a transaction.
 *
 * <p>The PostgreSQL FORCE RLS tenant context ({@code app.tenant_id}) is only set when a transaction
 * begins (transaction-local {@code set_config}). SQL executed without a transaction therefore runs
 * without tenant isolation: reads silently return no tenant rows, and writes fail on the RLS {@code
 * WITH CHECK} clause. In strict mode this inspector fails fast instead, so such a call site is
 * found during development and tests. In non-strict mode it logs a warning once per distinct SQL
 * statement, with the call site as stack trace.
 *
 * <p>Strictness is controlled by {@code emberone.db.require-transaction}; it is registered in
 * {@link TenantContextAutoConfiguration}.
 */
@Slf4j
public class TransactionRequiredStatementInspector implements StatementInspector {

    private final boolean strict;
    private final Set<String> reportedStatements = ConcurrentHashMap.newKeySet();

    public TransactionRequiredStatementInspector(boolean strict) {
        this.strict = strict;
    }

    @Override
    public String inspect(String sql) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            String message =
                    "SQL executed outside a transaction; RLS tenant context (app.tenant_id) is not"
                            + " applied: "
                            + sql;
            if (strict) {
                throw new IllegalStateException(message);
            }
            if (reportedStatements.add(sql)) {
                log.warn(message, new Exception("call site"));
            }
        }
        return sql;
    }
}
