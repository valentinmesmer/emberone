/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
 * Management module and runtime capability. Admin audience config needs only the module ({@link
 * #isModuleEnabled}); activating and using management rights also need {@code MANAGEMENT_ACCESS}
 * ({@link #isCapable}).
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

    /**
     * Whether the organization's effective standard has the management module. Gates admin audience
     * configuration (same pattern as course-offering types).
     */
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return false;
        }
        return effectiveStandardResolver
                .resolveForOrganization(organization)
                .map(
                        standard ->
                                enabledModuleResolver
                                        .forStandard(standard)
                                        .contains(Module.MANAGEMENT))
                .orElse(false);
    }

    /**
     * Whether the organization may activate management into other organizations: module enabled and
     * an active group carries {@code MANAGEMENT_ACCESS}.
     */
    @Transactional(readOnly = true)
    public boolean isCapable(Organization organization) {
        if (organization == null || organization.getId() == null || organization.isDeactivated()) {
            return false;
        }
        if (!isModuleEnabled(organization)) {
            return false;
        }
        return permissionGroupMappingRepository.countMappingsGrantingPermission(
                        organization.getId(), PermissionType.MANAGEMENT_ACCESS.getId())
                > 0;
    }
}
