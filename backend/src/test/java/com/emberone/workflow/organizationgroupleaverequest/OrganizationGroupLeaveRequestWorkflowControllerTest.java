/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationgroupleaverequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OrganizationGroupLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationGroupLeaveRequestWorkflowControllerTest {

    @Mock WorkflowService workflowService;
    @Mock TokenService tokenService;

    OrganizationGroupLeaveRequestWorkflowController controller;

    @BeforeEach
    void setUp() {
        controller =
                new OrganizationGroupLeaveRequestWorkflowController(workflowService, tokenService);
    }

    @Test
    void openRequiresUserId() {
        when(tokenService.requireUserId())
                .thenThrow(new IllegalStateException("No userId found in token"));

        assertThatThrownBy(
                        () -> controller.open(new OrganizationGroupLeaveRequestWorkflowPayload()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void openDelegatesToWorkflowService() {
        when(tokenService.requireUserId()).thenReturn(15L);
        OrganizationGroupLeaveRequestWorkflowPayload payload =
                OrganizationGroupLeaveRequestWorkflowPayload.builder()
                        .organizationId(20L)
                        .organizationGroupId(30L)
                        .build();
        WorkflowDto workflow = WorkflowDto.builder().id(8L).build();
        when(workflowService.open(
                        WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId(),
                        payload,
                        "USER",
                        15L))
                .thenReturn(workflow);

        ResponseEntity<WorkflowDto> response = controller.open(payload);

        assertThat(response.getBody()).isEqualTo(workflow);
    }
}
