/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationjoinrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationJoinRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationJoinRequestWorkflowControllerTest {

    @Mock WorkflowService workflowService;
    @Mock TokenService tokenService;

    OrganizationJoinRequestWorkflowController controller;

    @BeforeEach
    void setUp() {
        controller = new OrganizationJoinRequestWorkflowController(workflowService, tokenService);
    }

    @Test
    void openRequiresUserId() {
        when(tokenService.requireUserId())
                .thenThrow(new IllegalStateException("No userId found in token"));

        assertThatThrownBy(() -> controller.open(new OrganizationJoinRequestWorkflowPayload()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void openDelegatesToWorkflowService() {
        when(tokenService.requireUserId()).thenReturn(15L);
        OrganizationJoinRequestWorkflowPayload payload =
                OrganizationJoinRequestWorkflowPayload.builder().targetOrganizationId(20L).build();
        WorkflowDto workflow = WorkflowDto.builder().id(8L).build();
        when(workflowService.open(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId(), payload, "USER", 15L))
                .thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.open(payload);

        assertThat(response.getBody()).isEqualTo(workflow);
    }
}
