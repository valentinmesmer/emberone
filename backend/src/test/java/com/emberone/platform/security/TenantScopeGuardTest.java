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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TenantScopeGuardTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void bindSetsTenantContextWithoutJwtClaims() {
        assertThat(TenantScopeGuard.bind(42L)).isEqualTo(42L);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(42L);
    }

    @Test
    void requireActiveTenantMatchesRejectsMismatch() {
        TenantScopeGuard.bind(7L);

        assertThatThrownBy(() -> TenantScopeGuard.requireActiveTenantMatches(8L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void requireEntityInActiveTenantRejectsForeignEntity() {
        TenantScopeGuard.bind(7L);

        assertThatThrownBy(() -> TenantScopeGuard.requireEntityInActiveTenant(99L, "Standard", 1L))
                .hasMessageContaining("Standard not found");
    }
}
