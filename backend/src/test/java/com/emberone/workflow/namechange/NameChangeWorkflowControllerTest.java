/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.namechange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.NameChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class NameChangeWorkflowControllerTest {

    @Mock WorkflowService workflowService;
    @Mock TokenService tokenService;

    NameChangeWorkflowController controller;

    @BeforeEach
    void setUp() {
        controller = new NameChangeWorkflowController(workflowService, tokenService);
    }

    @Test
    void openDelegatesToWorkflowServiceWithUserOpener() {
        when(tokenService.requireUserId()).thenReturn(22L);
        NameChangeWorkflowPayload payload = new NameChangeWorkflowPayload();
        WorkflowDto workflow = WorkflowDto.builder().id(6L).build();
        when(workflowService.open(WorkflowType.NAME_CHANGE.getId(), payload, "USER", 22L))
                .thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.open(payload);

        assertThat(response.getBody()).isEqualTo(workflow);
    }
}
