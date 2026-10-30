/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.event.WorkflowConfigurationScope;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowConfigurationSyncServiceTest {

    @Mock WorkflowService workflowService;

    @InjectMocks WorkflowConfigurationSyncService service;

    @Test
    void onWorkflowConfigurationChangedDelegatesToWorkflowService() {
        WorkflowConfigurationChangedEvent event =
                WorkflowConfigurationChangedEvent.forOrganizations(1L, 2L, Set.of(10L, 11L));

        service.onWorkflowConfigurationChanged(event);

        verify(workflowService).reconcileOpenInstancesAfterConfigurationChange(event);
    }

    @Test
    void onWorkflowConfigurationChangedTenantWideReconcile() {
        WorkflowConfigurationChangedEvent event =
                WorkflowConfigurationChangedEvent.tenantWide(1L, 2L);

        service.onWorkflowConfigurationChanged(event);

        verify(workflowService).reconcileOpenInstancesAfterConfigurationChange(event);
    }

    @Test
    void onWorkflowConfigurationChangedNoOpsWhenEventIncomplete() {
        service.onWorkflowConfigurationChanged(null);
        service.onWorkflowConfigurationChanged(
                new WorkflowConfigurationChangedEvent(
                        null, 2L, WorkflowConfigurationScope.ORGANIZATIONS, Set.of(10L)));
        service.onWorkflowConfigurationChanged(
                new WorkflowConfigurationChangedEvent(
                        1L, null, WorkflowConfigurationScope.ORGANIZATIONS, Set.of(10L)));

        verifyNoInteractions(workflowService);
    }
}
