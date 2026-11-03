/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionGroupTypeTest {

    @Test
    void adminEffectivePermissionTypesIncludesEveryPermissionType() {
        List<PermissionType> effective =
                PermissionGroupType.ADMIN.effectivePermissionTypes(
                        List.of(PermissionType.USER_READ));

        assertThat(effective)
                .containsExactlyInAnyOrder(
                        Arrays.stream(PermissionType.values()).toArray(PermissionType[]::new));
        assertThat(effective).contains(PermissionType.MANAGEMENT_ACCESS);
    }

    @Test
    void nonAdminEffectivePermissionTypesUsesRequestedList() {
        List<PermissionType> requested =
                List.of(PermissionType.USER_READ, PermissionType.USER_WRITE);

        assertThat(PermissionGroupType.STANDARD.effectivePermissionTypes(requested))
                .containsExactlyElementsOf(requested);
        assertThat(PermissionGroupType.CUSTOM.effectivePermissionTypes(requested))
                .containsExactlyElementsOf(requested);
        assertThat(
                        PermissionGroupType.CUSTOM.effectivePermissionTypes(
                                List.of(PermissionType.MANAGEMENT_ACCESS)))
                .containsExactly(PermissionType.MANAGEMENT_ACCESS);
    }
}
