/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permissiongroupmapping.service;

import static org.mockito.Mockito.verify;

import com.emberone.organizationgroup.event.OrganizationGroupDeletingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationGroupDeletingMappingCleanupListenerTest {

    @Mock PermissionGroupMappingService permissionGroupMappingService;
    @InjectMocks OrganizationGroupDeletingMappingCleanupListener listener;

    @Test
    void onOrganizationGroupDeletingDelegatesToMappingService() {
        listener.onOrganizationGroupDeleting(new OrganizationGroupDeletingEvent(42L, 7L));

        verify(permissionGroupMappingService).deleteMappingsByOrganizationGroupId(42L);
    }
}
