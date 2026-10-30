/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.realtime.PermissionRealtimeNotifier;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementAudienceCleanupListenerTest {

    private static final long TENANT_ID = 7L;
    private static final long INCAPABLE_ORG_ID = 10L;
    private static final long CAPABLE_ORG_ID = 11L;

    @Mock ManagementAudienceRepository audienceRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock ManagementCapabilityService capabilityService;
    @Mock ManagementActivationService activationService;
    @Mock PermissionRealtimeNotifier permissionRealtimeNotifier;

    @InjectMocks ManagementAudienceCleanupListener listener;

    @Test
    void removesEntriesOnlyOfOrganizationsThatLostCapability() {
        Organization incapable = organization(INCAPABLE_ORG_ID);
        Organization capable = organization(CAPABLE_ORG_ID);
        ManagementAudience entry =
                ManagementAudience.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .managingOrganizationId(INCAPABLE_ORG_ID)
                        .kind(ManagementAudienceKind.TENANT)
                        .maxDurationMinutes(30)
                        .build();
        when(audienceRepository.findDistinctManagingOrganizationIds())
                .thenReturn(List.of(INCAPABLE_ORG_ID, CAPABLE_ORG_ID));
        when(organizationRepository.findById(INCAPABLE_ORG_ID)).thenReturn(Optional.of(incapable));
        when(organizationRepository.findById(CAPABLE_ORG_ID)).thenReturn(Optional.of(capable));
        when(capabilityService.isCapable(incapable)).thenReturn(false);
        when(capabilityService.isCapable(capable)).thenReturn(true);
        when(audienceRepository.findByManagingOrganizationIdOrderByIdAsc(INCAPABLE_ORG_ID))
                .thenReturn(List.of(entry));

        listener.onStandardEnabledModulesChanged(
                new StandardEnabledModulesChangedEvent(TENANT_ID, 3L));

        verify(audienceRepository).deleteAll(any(Iterable.class));
        verify(activationService).endSystemActivationsThrough(INCAPABLE_ORG_ID);
        verify(audienceRepository, never())
                .findByManagingOrganizationIdOrderByIdAsc(eq(CAPABLE_ORG_ID));
    }

    @Test
    void doesNothingWithoutTenant() {
        listener.onStandardEnabledModulesChanged(new StandardEnabledModulesChangedEvent(null, 3L));

        verify(audienceRepository, never()).findDistinctManagingOrganizationIds();
    }

    private static Organization organization(long id) {
        return Organization.builder().id(id).tenantId(TENANT_ID).name("Org " + id).build();
    }
}
