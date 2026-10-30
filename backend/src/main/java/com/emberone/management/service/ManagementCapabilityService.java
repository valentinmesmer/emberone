/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides whether an organization may manage other organizations: the management module is enabled
 * for its effective standard and one of its active groups carries {@code MANAGEMENT_ACCESS}.
 */
@Service
@RequiredArgsConstructor
public class ManagementCapabilityService {

    private final EffectiveStandardResolver effectiveStandardResolver;
    private final EnabledModuleResolver enabledModuleResolver;
    private final PermissionGroupMappingRepository permissionGroupMappingRepository;

    /**
     * Ids of the tenant's organizations that have a direct {@code MANAGEMENT_ACCESS} mapping. Only
     * these can be capable, so list views call {@link #isCapable} for them alone.
     */
    @Transactional(readOnly = true)
    public Set<Long> managementCandidateIds() {
        return Set.copyOf(
                permissionGroupMappingRepository.findOrganizationIdsGrantingPermission(
                        PermissionType.MANAGEMENT_ACCESS.getId()));
    }

    @Transactional(readOnly = true)
    public boolean isCapable(Organization organization) {
        if (organization == null || organization.getId() == null || organization.isDeactivated()) {
            return false;
        }
        boolean moduleEnabled =
                effectiveStandardResolver
                        .resolveForOrganization(organization)
                        .map(
                                standard ->
                                        enabledModuleResolver
                                                .forStandard(standard)
                                                .contains(Module.MANAGEMENT))
                        .orElse(false);
        if (!moduleEnabled) {
            return false;
        }
        return permissionGroupMappingRepository.countMappingsGrantingPermission(
                        organization.getId(), PermissionType.MANAGEMENT_ACCESS.getId())
                > 0;
    }
}
