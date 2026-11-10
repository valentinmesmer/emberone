/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.workflowdefinition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.workflow.dto.SaveWorkflowRoutingRuleRequest;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.emberone.workflow.dto.WorkflowRoutingRuleDto;
import com.emberone.workflow.service.WorkflowDefinitionService;
import java.util.List;
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

    @Test
    void routingRuleEndpointsDelegateToService() {
        SaveWorkflowRoutingRuleRequest request =
                SaveWorkflowRoutingRuleRequest.builder().subjectIds(List.of(5L)).build();
        WorkflowRoutingRuleDto rule =
                WorkflowRoutingRuleDto.builder().id(8L).subjectIds(List.of(5L)).build();
        when(definitionService.getRoutingRule(3L, 10L, 8L)).thenReturn(rule);
        when(definitionService.createRoutingRule(3L, 10L, request)).thenReturn(rule);
        when(definitionService.updateRoutingRule(3L, 10L, 8L, request)).thenReturn(rule);

        assertThat(controller.getRoutingRule(3L, 10L, 8L).getBody()).isEqualTo(rule);
        ResponseEntity<WorkflowRoutingRuleDto> created =
                controller.createRoutingRule(3L, 10L, request);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).isEqualTo(rule);
        assertThat(controller.updateRoutingRule(3L, 10L, 8L, request).getBody()).isEqualTo(rule);
        assertThat(controller.deleteRoutingRule(3L, 10L, 8L).getStatusCode().value())
                .isEqualTo(204);
        verify(definitionService).deleteRoutingRule(3L, 10L, 8L);
    }

    @Test
    void routingRuleRequestRequiresAtLeastOneSubject() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertThat(
                            validator.validate(
                                    SaveWorkflowRoutingRuleRequest.builder()
                                            .subjectIds(List.of())
                                            .build()))
                    .hasSize(1);
            assertThat(
                            validator.validate(
                                    SaveWorkflowRoutingRuleRequest.builder()
                                            .subjectIds(List.of(5L))
                                            .build()))
                    .isEmpty();
        }
    }
}
