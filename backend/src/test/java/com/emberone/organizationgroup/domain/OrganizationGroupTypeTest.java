/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationGroupTypeTest {

    @Test
    void systemTypesAreDetected() {
        assertThat(OrganizationGroupType.LEADERSHIP.isSystem()).isTrue();
        assertThat(OrganizationGroupType.ALL_MEMBERS.isSystem()).isTrue();
        assertThat(OrganizationGroupType.CUSTOM.isSystem()).isFalse();
    }

    @Test
    void workflowApproverEligibility() {
        assertThat(OrganizationGroupType.LEADERSHIP.canApproveWorkflows()).isTrue();
        assertThat(OrganizationGroupType.CUSTOM.canApproveWorkflows()).isTrue();
        assertThat(OrganizationGroupType.ALL_MEMBERS.canApproveWorkflows()).isFalse();
    }

    @Test
    void userInterfaceVisibility() {
        assertThat(OrganizationGroupType.LEADERSHIP.isVisibleInUserInterface()).isTrue();
        assertThat(OrganizationGroupType.CUSTOM.isVisibleInUserInterface()).isTrue();
        assertThat(OrganizationGroupType.ALL_MEMBERS.isVisibleInUserInterface()).isFalse();
        assertThat(OrganizationGroupType.userInterfaceVisibleTypes())
                .containsExactly(OrganizationGroupType.LEADERSHIP, OrganizationGroupType.CUSTOM);
    }
}
