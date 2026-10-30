/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.emberone.management.service;

import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.event.OrganizationGroupDeletingEvent;
import com.emberone.permission.event.OrganizationGroupPermissionsChangedEvent;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permission.realtime.PermissionRealtimeNotifier;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.standard.event.OrganizationStandardOverrideChangedEvent;
import com.emberone.standard.event.OrganizationTypeStandardAssignedEvent;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Drops audience entries of managing organizations that lost their capability, and ends their open
 * activations ({@code SYSTEM}), so stale entries do not come back when the capability is restored.
 * The access check does not rely on this; it re-evaluates the capability on every request.
 */
@Service
@RequiredArgsConstructor
public class ManagementAudienceCleanupListener {
    private final ManagementAudienceRepository audienceRepository;
    private final OrganizationRepository organizationRepository;
    private final ManagementCapabilityService capabilityService;
    private final ManagementActivationService activationService;
    private final PermissionRealtimeNotifier permissionRealtimeNotifier;

    @AfterCommitEventListener
    public void onStandardEnabledModulesChanged(StandardEnabledModulesChangedEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationTypeStandardAssigned(OrganizationTypeStandardAssignedEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationStandardOverrideChanged(
            OrganizationStandardOverrideChangedEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onPermissionGroupPermissionsChanged(PermissionGroupPermissionsChangedEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    /** Archiving or deleting a group can remove the managing capability of its organization. */
    @AfterCommitEventListener
    public void onOrganizationGroupDeleting(OrganizationGroupDeletingEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationGroupPermissionsChanged(
            OrganizationGroupPermissionsChangedEvent event) {
        removeEntriesOfIncapableOrganizations(event.tenantId());
    }

    /**
     * Checks every managing organization of the tenant that has entries. The set is small (only
     * configured managing organizations), so no per-event narrowing is needed.
     */
    private void removeEntriesOfIncapableOrganizations(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        for (Long managingOrganizationId :
                audienceRepository.findDistinctManagingOrganizationIds()) {
            organizationRepository
                    .findById(managingOrganizationId)
                    .filter(organization -> tenantId.equals(organization.getTenantId()))
                    .filter(organization -> !capabilityService.isCapable(organization))
                    .ifPresent(
                            organization -> {
                                audienceRepository.deleteAll(
                                        audienceRepository.findByManagingOrganizationIdOrderByIdAsc(
                                                organization.getId()));
                                for (Long userId :
                                        activationService.endSystemActivationsThrough(
                                                organization.getId())) {
                                    permissionRealtimeNotifier.notifyPermissionsChanged(
                                            tenantId, userId);
                                }
                            });
        }
    }
}
