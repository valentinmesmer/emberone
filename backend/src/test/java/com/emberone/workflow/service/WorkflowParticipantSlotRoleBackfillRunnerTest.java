/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.platform.tenant.TenantBatchRunner;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowParticipantSlotRoleBackfillRunnerTest {

    @Mock TenantBatchRunner tenantBatchRunner;
    @Mock WorkflowService workflowService;

    @Test
    void backfillProcessesEachCandidateAndSurvivesAFailingInstance() {
        doAnswer(
                        invocation -> {
                            invocation.<Consumer<Long>>getArgument(0).accept(7L);
                            return null;
                        })
                .when(tenantBatchRunner)
                .forEachTenant(any());
        when(workflowService.findWorkflowIdsAwaitingSlotRoleBackfill())
                .thenReturn(List.of(1L, 2L, 3L));
        when(workflowService.backfillApproverSlotRoles(1L)).thenReturn(1);
        when(workflowService.backfillApproverSlotRoles(2L))
                .thenThrow(new IllegalStateException("cannot resolve"));
        when(workflowService.backfillApproverSlotRoles(3L)).thenReturn(2);

        new WorkflowParticipantSlotRoleBackfillRunner(tenantBatchRunner, workflowService)
                .backfill();

        verify(workflowService).backfillApproverSlotRoles(1L);
        verify(workflowService).backfillApproverSlotRoles(2L);
        verify(workflowService).backfillApproverSlotRoles(3L);
    }

    @Test
    void backfillDoesNothingWhenNoInstanceNeedsMigration() {
        doAnswer(
                        invocation -> {
                            invocation.<Consumer<Long>>getArgument(0).accept(7L);
                            return null;
                        })
                .when(tenantBatchRunner)
                .forEachTenant(any());
        when(workflowService.findWorkflowIdsAwaitingSlotRoleBackfill()).thenReturn(List.of());

        new WorkflowParticipantSlotRoleBackfillRunner(tenantBatchRunner, workflowService)
                .backfill();

        verify(workflowService, never()).backfillApproverSlotRoles(any());
    }
}
