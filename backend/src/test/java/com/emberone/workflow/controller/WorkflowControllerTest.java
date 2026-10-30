/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.workflow.dto.WorkflowDecisionRequest;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock WorkflowService workflowService;

    @InjectMocks WorkflowController controller;

    @Test
    void getWorkflowDelegatesToService() {
        WorkflowDto workflow = WorkflowDto.builder().id(5L).build();
        when(workflowService.getWorkflow(5L)).thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.getWorkflow(5L);

        assertThat(response.getBody()).isEqualTo(workflow);
    }

    @Test
    void decideDelegatesRequestToService() {
        WorkflowDecisionRequest request = new WorkflowDecisionRequest();
        WorkflowDto workflow = WorkflowDto.builder().id(5L).build();
        when(workflowService.decide(5L, request)).thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.decide(5L, request);

        assertThat(response.getBody()).isEqualTo(workflow);
    }

    @Test
    void cancelDelegatesToService() {
        WorkflowDto workflow = WorkflowDto.builder().id(5L).build();
        when(workflowService.cancel(5L)).thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.cancel(5L);

        assertThat(response.getBody()).isEqualTo(workflow);
        verify(workflowService).cancel(5L);
    }
}
