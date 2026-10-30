/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmberOneAppRoleTest {

    @Test
    void fromRoleNameResolvesKnownRolesCaseInsensitive() {
        assertThat(EmberOneAppRole.fromRoleName("tenant_admin"))
                .contains(EmberOneAppRole.TENANT_ADMIN);
        assertThat(EmberOneAppRole.fromRoleName("  USER ")).contains(EmberOneAppRole.USER);
    }

    @Test
    void fromRoleNameReturnsEmptyForUnknownOrBlank() {
        assertThat(EmberOneAppRole.fromRoleName("ORG_ADMIN")).isEmpty();
        assertThat(EmberOneAppRole.fromRoleName("CUSTOM")).isEmpty();
        assertThat(EmberOneAppRole.fromRoleName(null)).isEmpty();
        assertThat(EmberOneAppRole.fromRoleName(" ")).isEmpty();
    }

    @Test
    void actorMayAssignRoleRespectsRank() {
        assertThat(
                        EmberOneAppRole.actorMayAssignRole(
                                EmberOneAppRole.GLOBAL_ADMIN, EmberOneAppRole.USER))
                .isTrue();
        assertThat(
                        EmberOneAppRole.actorMayAssignRole(
                                EmberOneAppRole.TENANT_ADMIN, EmberOneAppRole.USER))
                .isTrue();
        assertThat(
                        EmberOneAppRole.actorMayAssignRole(
                                EmberOneAppRole.USER, EmberOneAppRole.TENANT_ADMIN))
                .isFalse();
        assertThat(EmberOneAppRole.actorMayAssignRole(null, EmberOneAppRole.USER)).isFalse();
    }
}
