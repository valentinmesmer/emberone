/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionRequiredStatementInspectorTest {

    private static final String SQL = "select * from users";

    @AfterEach
    void resetTransactionState() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void strictModeRejectsSqlOutsideTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionRequiredStatementInspector inspector =
                new TransactionRequiredStatementInspector(true);

        assertThatThrownBy(() -> inspector.inspect(SQL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.tenant_id")
                .hasMessageContaining(SQL);
    }

    @Test
    void strictModeAllowsSqlInsideTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionRequiredStatementInspector inspector =
                new TransactionRequiredStatementInspector(true);

        assertThat(inspector.inspect(SQL)).isEqualTo(SQL);
    }

    @Test
    void nonStrictModeReturnsSqlUnchangedOutsideTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionRequiredStatementInspector inspector =
                new TransactionRequiredStatementInspector(false);

        assertThat(inspector.inspect(SQL)).isEqualTo(SQL);
    }

    @Test
    void nonStrictModeKeepsReturningRepeatedSqlOutsideTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionRequiredStatementInspector inspector =
                new TransactionRequiredStatementInspector(false);

        assertThat(inspector.inspect(SQL)).isEqualTo(SQL);
        assertThat(inspector.inspect(SQL)).isEqualTo(SQL);
    }
}
