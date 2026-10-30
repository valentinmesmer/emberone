/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.list.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.web.list.options.ListFilterOptionsAuthorization;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationSubtreeOrganizationIdFilterOptionsProviderTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock ListFilterOptionsAuthorization listFilterOptionsAuthorization;

    @InjectMocks OrganizationSubtreeOrganizationIdFilterOptionsProvider provider;

    @Test
    void onlyOffersOrganizationsCallerMayRead() {
        Organization root = Organization.builder().id(10L).tenantId(1L).name("Root").build();
        Organization childAllowed =
                Organization.builder().id(11L).tenantId(1L).name("Child A").build();
        Organization childDenied =
                Organization.builder().id(12L).tenantId(1L).name("Child B").build();
        childAllowed.setParentOrganization(root);
        childDenied.setParentOrganization(root);

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(root));
        when(organizationRepository.findAllWithParentFetch())
                .thenReturn(List.of(root, childAllowed, childDenied));
        when(listFilterOptionsAuthorization.mayExposeOrganizationForOrgWorkspaceFilter(
                        10L, PermissionType.USER_READ))
                .thenReturn(true);
        when(listFilterOptionsAuthorization.mayExposeOrganizationForOrgWorkspaceFilter(
                        11L, PermissionType.USER_READ))
                .thenReturn(true);
        when(listFilterOptionsAuthorization.mayExposeOrganizationForOrgWorkspaceFilter(
                        12L, PermissionType.USER_READ))
                .thenReturn(false);

        var options = provider.resolve(ListFilterOptionsContext.organization(10L));

        assertThat(options)
                .extracting(option -> option.value())
                .containsExactlyInAnyOrder("10", "11");
        assertThat(options).extracting(option -> option.value()).doesNotContain("12");
    }
}
