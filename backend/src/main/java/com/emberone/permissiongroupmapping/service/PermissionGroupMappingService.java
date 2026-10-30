/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroupmapping.service;

import com.emberone.permission.event.OrganizationGroupPermissionsChangedEvent;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroup.exception.PermissionGroupNotFoundException;
import com.emberone.permissiongroup.repository.PermissionGroupRepository;
import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionGroupMappingService {

    private final PermissionGroupMappingRepository mappingRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a standard-managed mapping when materializing a standard's mapping template. If a
     * mapping already exists it is left untouched (a manual mapping is never downgraded), so
     * removing the template later will not delete a user-created mapping.
     */
    @Transactional
    public void createStandardMapping(Long organizationGroupId, Long permissionGroupId) {
        PermissionGroup permissionGroup =
                permissionGroupRepository
                        .findById(permissionGroupId)
                        .orElseThrow(() -> new PermissionGroupNotFoundException(permissionGroupId));

        if (mappingRepository.existsByOrganizationGroupIdAndPermissionGroup_Id(
                organizationGroupId, permissionGroupId)) {
            log.debug(
                    "Mapping already exists between organization group {} and permission group {}",
                    organizationGroupId,
                    permissionGroupId);
            return;
        }

        PermissionGroupMapping mapping =
                PermissionGroupMapping.builder()
                        .tenantId(permissionGroup.getTenantId())
                        .organizationGroupId(organizationGroupId)
                        .permissionGroup(permissionGroup)
                        .standardManaged(true)
                        .build();

        mappingRepository.save(mapping);
        eventPublisher.publishEvent(
                new OrganizationGroupPermissionsChangedEvent(
                        permissionGroup.getTenantId(), organizationGroupId));
        log.info(
                "Created mapping between organization group {} and permission group {}",
                organizationGroupId,
                permissionGroupId);
    }

    /**
     * Remove a standard-managed mapping between an organization group and a permission group.
     * Manual mappings (standardManaged = false) are left untouched so no user-created mapping is
     * lost.
     */
    @Transactional
    public void deleteStandardMapping(Long organizationGroupId, Long permissionGroupId) {
        mappingRepository.findByOrganizationGroupId(organizationGroupId).stream()
                .filter(
                        m ->
                                m.getPermissionGroup().getId().equals(permissionGroupId)
                                        && m.isStandardManaged())
                .findFirst()
                .ifPresent(
                        mapping -> {
                            mappingRepository.delete(mapping);
                            eventPublisher.publishEvent(
                                    new OrganizationGroupPermissionsChangedEvent(
                                            mapping.getTenantId(), organizationGroupId));
                            log.info(
                                    "Deleted standard-managed mapping between organization group {} and permission group {}",
                                    organizationGroupId,
                                    permissionGroupId);
                        });
    }

    @Transactional
    public void deleteMappingsByOrganizationGroupId(Long organizationGroupId) {
        Long tenantId =
                mappingRepository.findByOrganizationGroupId(organizationGroupId).stream()
                        .map(PermissionGroupMapping::getTenantId)
                        .findFirst()
                        .orElse(null);
        mappingRepository.deleteByOrganizationGroupId(organizationGroupId);
        if (tenantId != null) {
            eventPublisher.publishEvent(
                    new OrganizationGroupPermissionsChangedEvent(tenantId, organizationGroupId));
        }
        log.info("Deleted all mappings for organization group {}", organizationGroupId);
    }

    @Transactional
    public void deleteMappingsByPermissionGroupId(Long permissionGroupId) {
        Long tenantId =
                permissionGroupRepository
                        .findById(permissionGroupId)
                        .map(PermissionGroup::getTenantId)
                        .orElse(null);
        mappingRepository.deleteByPermissionGroupId(permissionGroupId);
        if (tenantId != null) {
            eventPublisher.publishEvent(
                    new PermissionGroupPermissionsChangedEvent(tenantId, permissionGroupId));
        }
        log.info("Deleted all mappings for permission group {}", permissionGroupId);
    }
}
