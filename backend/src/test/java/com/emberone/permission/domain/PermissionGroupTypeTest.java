/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionGroupTypeTest {

    @Test
    void adminEffectivePermissionTypesIncludesAllPermissionsExceptManagementAccess() {
        List<PermissionType> effective =
                PermissionGroupType.ADMIN.effectivePermissionTypes(
                        List.of(PermissionType.ORGANIZATION_READ));

        assertThat(effective)
                .containsExactlyInAnyOrder(
                        Arrays.stream(PermissionType.values())
                                .filter(type -> type != PermissionType.MANAGEMENT_ACCESS)
                                .toArray(PermissionType[]::new));
        assertThat(effective).doesNotContain(PermissionType.MANAGEMENT_ACCESS);
    }

    @Test
    void managementAccessRemainsGrantableToNonAdminGroupsOnly() {
        List<PermissionType> requested = List.of(PermissionType.MANAGEMENT_ACCESS);

        assertThat(PermissionGroupType.STANDARD.effectivePermissionTypes(requested))
                .containsExactly(PermissionType.MANAGEMENT_ACCESS);
        assertThat(PermissionGroupType.ADMIN.effectivePermissionTypes(requested))
                .doesNotContain(PermissionType.MANAGEMENT_ACCESS);
    }

    @Test
    void nonAdminEffectivePermissionTypesUsesRequestedList() {
        List<PermissionType> requested =
                List.of(PermissionType.USER_READ, PermissionType.USER_WRITE);

        assertThat(PermissionGroupType.STANDARD.effectivePermissionTypes(requested))
                .containsExactlyElementsOf(requested);
        assertThat(PermissionGroupType.CUSTOM.effectivePermissionTypes(requested))
                .containsExactlyElementsOf(requested);
    }
}
