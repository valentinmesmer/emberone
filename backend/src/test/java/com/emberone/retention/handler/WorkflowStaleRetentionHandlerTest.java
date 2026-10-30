/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.workflow.service.WorkflowService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowStaleRetentionHandlerTest {

    @Mock WorkflowService workflowService;

    @InjectMocks WorkflowStaleRetentionHandler handler;

    @Test
    void failingCancellationDoesNotStopOtherStaleWorkflows() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowService.findStaleWorkflowIds(cutoff)).thenReturn(List.of(1L, 2L, 3L));
        when(workflowService.cancelStaleWorkflow(1L, cutoff)).thenReturn(true);
        when(workflowService.cancelStaleWorkflow(2L, cutoff))
                .thenThrow(new IllegalStateException("handler callback failed"));
        when(workflowService.cancelStaleWorkflow(3L, cutoff)).thenReturn(true);

        int cancelled = handler.apply(cutoff);

        assertThat(cancelled).isEqualTo(2);
        verify(workflowService).cancelStaleWorkflow(3L, cutoff);
    }

    @Test
    void workflowLockedByParallelRunIsNotCounted() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowService.findStaleWorkflowIds(cutoff)).thenReturn(List.of(1L));
        when(workflowService.cancelStaleWorkflow(1L, cutoff)).thenReturn(false);

        assertThat(handler.apply(cutoff)).isZero();
    }
}
