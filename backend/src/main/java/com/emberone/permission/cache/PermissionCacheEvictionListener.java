/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.cache;

import com.emberone.permission.event.OrganizationGroupPermissionsChangedEvent;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionCacheEvictionListener {

    private final UserPermissionCacheEvictor userPermissionCacheEvictor;
    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final PermissionGroupMappingRepository mappingRepository;

    @AfterCommitCallback
    public void onUserPermissionsChanged(UserPermissionsChangedEvent event) {
        if (event.userId() != null) {
            userPermissionCacheEvictor.evictUser(event.tenantId(), event.userId());
        }
    }

    @AfterCommitEventListener(readOnly = true)
    public void onOrganizationGroupPermissionsChanged(
            OrganizationGroupPermissionsChangedEvent event) {
        evictOrganizationGroupMembers(event.organizationGroupId());
    }

    @AfterCommitEventListener(readOnly = true)
    public void onPermissionGroupPermissionsChanged(PermissionGroupPermissionsChangedEvent event) {
        evictPermissionGroupMembers(event.permissionGroupId());
    }

    private void evictOrganizationGroupMembers(Long organizationGroupId) {
        if (organizationGroupId == null) {
            return;
        }
        Instant now = Instant.now();
        membershipRepository.findByOrganizationGroup_IdWithUser(organizationGroupId).stream()
                .filter(membership -> membership.isActiveAt(now))
                .forEach(
                        membership ->
                                userPermissionCacheEvictor.evictUser(
                                        membership.getTenantId(), membership.getUser().getId()));
    }

    private void evictPermissionGroupMembers(Long permissionGroupId) {
        if (permissionGroupId == null) {
            return;
        }
        List<PermissionGroupMapping> mappings =
                mappingRepository.findByPermissionGroup_Id(permissionGroupId);
        if (mappings.isEmpty()) {
            return;
        }
        Set<String> evictedCacheKeys = new HashSet<>();
        Instant now = Instant.now();
        for (PermissionGroupMapping mapping : mappings) {
            membershipRepository
                    .findByOrganizationGroup_IdWithUser(mapping.getOrganizationGroupId())
                    .stream()
                    .filter(membership -> membership.isActiveAt(now))
                    .filter(
                            membership ->
                                    evictedCacheKeys.add(
                                            membership.getTenantId()
                                                    + ":"
                                                    + membership.getUser().getId()))
                    .forEach(
                            membership ->
                                    userPermissionCacheEvictor.evictUser(
                                            membership.getTenantId(),
                                            membership.getUser().getId()));
        }
    }
}
