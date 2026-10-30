/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.realtime;

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

/**
 * Sole subscriber of the permission-change events; callers publish an event instead of depending on
 * this service directly so the realtime push mechanism stays swappable.
 */
@Service
@RequiredArgsConstructor
public class PermissionChangeRealtimeService {

    private final PermissionRealtimeNotifier notifier;
    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final PermissionGroupMappingRepository mappingRepository;

    @AfterCommitCallback
    public void onUserPermissionsChanged(UserPermissionsChangedEvent event) {
        notifyUser(event.tenantId(), event.userId());
    }

    @AfterCommitEventListener(readOnly = true)
    public void onOrganizationGroupPermissionsChanged(
            OrganizationGroupPermissionsChangedEvent event) {
        notifyOrganizationGroupMembers(event.tenantId(), event.organizationGroupId());
    }

    @AfterCommitEventListener(readOnly = true)
    public void onPermissionGroupPermissionsChanged(PermissionGroupPermissionsChangedEvent event) {
        notifyPermissionGroupMembers(event.tenantId(), event.permissionGroupId());
    }

    private void notifyUser(Long tenantId, Long userId) {
        notifier.notifyPermissionsChanged(tenantId, userId);
    }

    private void notifyOrganizationGroupMembers(Long tenantId, Long organizationGroupId) {
        if (tenantId == null || organizationGroupId == null) {
            return;
        }
        Instant now = Instant.now();
        membershipRepository.findByOrganizationGroup_IdWithUser(organizationGroupId).stream()
                .filter(membership -> membership.isActiveAt(now))
                .map(membership -> membership.getUser().getId())
                .distinct()
                .forEach(userId -> notifier.notifyPermissionsChanged(tenantId, userId));
    }

    private void notifyPermissionGroupMembers(Long tenantId, Long permissionGroupId) {
        if (tenantId == null || permissionGroupId == null) {
            return;
        }
        List<PermissionGroupMapping> mappings =
                mappingRepository.findByPermissionGroup_Id(permissionGroupId);
        if (mappings.isEmpty()) {
            return;
        }
        Set<Long> notifiedUserIds = new HashSet<>();
        Instant now = Instant.now();
        for (PermissionGroupMapping mapping : mappings) {
            membershipRepository
                    .findByOrganizationGroup_IdWithUser(mapping.getOrganizationGroupId())
                    .stream()
                    .filter(membership -> membership.isActiveAt(now))
                    .map(membership -> membership.getUser().getId())
                    .filter(notifiedUserIds::add)
                    .forEach(userId -> notifier.notifyPermissionsChanged(tenantId, userId));
        }
    }
}
