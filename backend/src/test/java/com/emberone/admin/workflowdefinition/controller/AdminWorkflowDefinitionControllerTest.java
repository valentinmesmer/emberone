/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowdefinition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.emberone.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminWorkflowDefinitionControllerTest {

    @Mock WorkflowDefinitionService definitionService;

    @InjectMocks AdminWorkflowDefinitionController controller;

    @Test
    void getTenantDefaultDelegatesToService() {
        TenantWorkflowDefinitionDto definition =
                TenantWorkflowDefinitionDto.builder().id(1L).build();
        when(definitionService.getTenantDefault(3L, 10L)).thenReturn(definition);

        ResponseEntity<TenantWorkflowDefinitionDto> response = controller.getTenantDefault(3L, 10L);

        assertThat(response.getBody()).isEqualTo(definition);
    }

    @Test
    void updateTenantDefaultDelegatesToService() {
        UpdateWorkflowDefinitionRequest request =
                UpdateWorkflowDefinitionRequest.builder().staticApproversExclusive(true).build();
        TenantWorkflowDefinitionDto definition =
                TenantWorkflowDefinitionDto.builder().id(1L).build();
        when(definitionService.updateTenantDefault(3L, 10L, request)).thenReturn(definition);

        ResponseEntity<TenantWorkflowDefinitionDto> response =
                controller.updateTenantDefault(3L, 10L, request);

        assertThat(response.getBody()).isEqualTo(definition);
    }
}
