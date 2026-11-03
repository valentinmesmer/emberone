/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
 * When the management module is off, drops audience entries. When the organization is no longer
 * capable ({@code MANAGEMENT_ACCESS} gone or module off), ends open activations ({@code SYSTEM}).
 * Hygiene only; the access check re-evaluates on every request.
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
        cleanupManagingOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationTypeStandardAssigned(OrganizationTypeStandardAssignedEvent event) {
        cleanupManagingOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationStandardOverrideChanged(
            OrganizationStandardOverrideChangedEvent event) {
        cleanupManagingOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onPermissionGroupPermissionsChanged(PermissionGroupPermissionsChangedEvent event) {
        cleanupManagingOrganizations(event.tenantId());
    }

    /** Archiving or deleting a group can remove the managing capability of its organization. */
    @AfterCommitEventListener
    public void onOrganizationGroupDeleting(OrganizationGroupDeletingEvent event) {
        cleanupManagingOrganizations(event.tenantId());
    }

    @AfterCommitEventListener
    public void onOrganizationGroupPermissionsChanged(
            OrganizationGroupPermissionsChangedEvent event) {
        cleanupManagingOrganizations(event.tenantId());
    }

    /**
     * Checks every managing organization of the tenant that has entries. The set is small (only
     * configured managing organizations), so no per-event narrowing is needed.
     */
    private void cleanupManagingOrganizations(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        for (Long managingOrganizationId :
                audienceRepository.findDistinctManagingOrganizationIds()) {
            organizationRepository
                    .findById(managingOrganizationId)
                    .filter(organization -> tenantId.equals(organization.getTenantId()))
                    .ifPresent(organization -> {
                        if (!capabilityService.isModuleEnabled(organization)) {
                            audienceRepository.deleteAll(
                                    audienceRepository.findByManagingOrganizationIdOrderByIdAsc(
                                            organization.getId()));
                        }
                        if (!capabilityService.isCapable(organization)) {
                            for (Long userId :
                                    activationService.endSystemActivationsThrough(
                                            organization.getId())) {
                                permissionRealtimeNotifier.notifyPermissionsChanged(
                                        tenantId, userId);
                            }
                        }
                    });
        }
    }
}
