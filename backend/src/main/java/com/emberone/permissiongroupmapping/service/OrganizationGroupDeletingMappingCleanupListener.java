/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permissiongroupmapping.service;

import com.emberone.organizationgroup.event.OrganizationGroupDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationGroupDeletingMappingCleanupListener {

    private final PermissionGroupMappingService permissionGroupMappingService;

    /**
     * Runs synchronously in the deleting/archiving transaction so mapping cleanup rolls back
     * together with the organization-group change if anything fails.
     */
    @EventListener
    public void onOrganizationGroupDeleting(OrganizationGroupDeletingEvent event) {
        permissionGroupMappingService.deleteMappingsByOrganizationGroupId(
                event.organizationGroupId());
    }
}
