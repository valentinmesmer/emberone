/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementCapabilityServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;

    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock EnabledModuleResolver enabledModuleResolver;
    @Mock PermissionGroupMappingRepository permissionGroupMappingRepository;

    @InjectMocks ManagementCapabilityService service;

    private final Standard standard = mock(Standard.class);

    @Test
    void capableWhenModuleIsEnabledAndAGroupMapsManagementAccess() {
        stubModules(EnumSet.allOf(Module.class));
        when(permissionGroupMappingRepository.countMappingsGrantingPermission(
                        ORG_ID, PermissionType.MANAGEMENT_ACCESS.getId()))
                .thenReturn(1L);

        assertThat(service.isCapable(organization(null))).isTrue();
    }

    @Test
    void notCapableWhenModuleIsDisabledForTheStandard() {
        stubModules(EnumSet.complementOf(EnumSet.of(Module.MANAGEMENT)));

        assertThat(service.isCapable(organization(null))).isFalse();
        verifyNoInteractions(permissionGroupMappingRepository);
    }

    @Test
    void notCapableWithoutEffectiveStandard() {
        when(effectiveStandardResolver.resolveForOrganization(any())).thenReturn(Optional.empty());

        assertThat(service.isCapable(organization(null))).isFalse();
        verifyNoInteractions(permissionGroupMappingRepository);
    }

    @Test
    void notCapableWhenNoGroupMapsManagementAccess() {
        stubModules(EnumSet.allOf(Module.class));
        when(permissionGroupMappingRepository.countMappingsGrantingPermission(anyLong(), anyLong()))
                .thenReturn(0L);

        assertThat(service.isCapable(organization(null))).isFalse();
    }

    @Test
    void notCapableWhenOrganizationIsDeactivated() {
        assertThat(service.isCapable(organization(Instant.now()))).isFalse();
        verifyNoInteractions(effectiveStandardResolver);
    }

    private void stubModules(EnumSet<Module> modules) {
        when(effectiveStandardResolver.resolveForOrganization(any()))
                .thenReturn(Optional.of(standard));
        when(enabledModuleResolver.forStandard(standard)).thenReturn(modules);
    }

    private static Organization organization(Instant deactivatedAt) {
        return Organization.builder()
                .id(ORG_ID)
                .tenantId(TENANT_ID)
                .name("Geschaeftsstelle")
                .deactivatedAt(deactivatedAt)
                .build();
    }
}
