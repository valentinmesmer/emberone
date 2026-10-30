/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void storesAndClearsTenantAndUserContext() {
        TenantContextHolder.setTenantId(42L);
        TenantContextHolder.setUserId(7L);

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(42L);
        assertThat(TenantContextHolder.getUserId()).isEqualTo(7L);

        TenantContextHolder.clear();

        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getUserId()).isNull();
    }

    @Test
    void globalAdminReadFlagIsClearedWithTheContext() {
        assertThat(TenantContextHolder.isGlobalAdminRead()).isFalse();

        TenantContextHolder.setGlobalAdminRead(true);
        assertThat(TenantContextHolder.isGlobalAdminRead()).isTrue();

        TenantContextHolder.clear();
        assertThat(TenantContextHolder.isGlobalAdminRead()).isFalse();
    }
}
