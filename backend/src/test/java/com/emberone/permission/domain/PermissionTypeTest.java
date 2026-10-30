/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PermissionTypeTest {

    @Test
    void fromId_roundTripsEveryPermission() {
        for (PermissionType permission : PermissionType.values()) {
            assertThat(PermissionType.fromId(permission.getId())).isSameAs(permission);
        }
    }

    @Test
    void fromId_handlesNullAndRejectsUnknownIds() {
        assertThat(PermissionType.fromId(null)).isNull();
        assertThatThrownBy(() -> PermissionType.fromId(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown PermissionType id: 999");
    }
}
