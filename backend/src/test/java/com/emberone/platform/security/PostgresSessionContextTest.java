/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresSessionContextTest {

    @Mock Connection connection;
    @Mock PreparedStatement setStatement;
    @Mock PreparedStatement resetStatement;

    @Test
    void setsTransactionLocalGucsWhenTenantAndUserPresent() throws SQLException {
        when(connection.prepareStatement(anyString()))
                .thenAnswer(
                        invocation -> {
                            String sql = invocation.getArgument(0, String.class);
                            if (sql.contains("set_config")) {
                                return setStatement;
                            }
                            return resetStatement;
                        });

        PostgresSessionContext.apply(connection, 7L, 42L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("app.tenant_id").contains("app.user_id");
        verify(setStatement).setString(1, "7");
        verify(setStatement).setString(2, "42");
    }

    @Test
    void setsRetentionFlag() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(setStatement);

        PostgresSessionContext.setRetention(connection, true);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("set_config");
        verify(setStatement).setString(1, "app.retention");
        verify(setStatement).setString(2, "true");
    }

    @Test
    void resetsMissingValues() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(resetStatement);

        PostgresSessionContext.apply(connection, null, null);

        verify(connection).prepareStatement("RESET app.tenant_id");
        verify(connection).prepareStatement("RESET app.user_id");
    }

    @Test
    void opensGlobalAdminReadForTheTransactionOnly() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(setStatement);

        PostgresSessionContext.applyGlobalAdminRead(connection);

        verify(connection).prepareStatement("SELECT set_config(?, ?, true)");
        verify(setStatement).setString(1, "app.global_admin");
        verify(setStatement).setString(2, "true");
    }
}
