/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowSelfApproveServiceTest {

    @Mock WorkflowDefinitionService definitionService;
    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock WorkflowHandler<Object> handler;
    @InjectMocks WorkflowSelfApproveService service;

    @Test
    void resolvesCurrentDefinitionFromInstanceOrganization() {
        Workflow workflow =
                Workflow.builder().workflowTypeId(5L).openedByUserId(1L).tenantId(7L).build();
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveInstanceOrganization(workflow)).thenReturn(10L);
        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(true).build());

        assertThat(service.isAllowed(workflow)).isTrue();
        verify(definitionService).resolveEffectiveDefinition(5L, 10L);
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void resolvesDefinitionFromInstanceOrganizationWhenSecurityContextUnavailable() {
        Workflow workflow =
                Workflow.builder().workflowTypeId(13L).openedByUserId(1L).tenantId(7L).build();
        doReturn(handler).when(handlerRegistry).require(13L);
        when(handler.resolveInstanceOrganization(workflow)).thenReturn(20L);
        when(definitionService.resolveEffectiveDefinition(13L, 20L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(true).build());

        assertThat(service.isAllowed(workflow)).isTrue();
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void missingContextOrganizationDisallowsSelfApprove() {
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveSecurityContextOrganization(1L, 7L)).thenReturn(null);

        assertThat(service.isAllowed(5L, 1L, 7L)).isFalse();
    }

    @Test
    void handlerOrDefinitionFailureFailsClosed() {
        when(handlerRegistry.require(5L)).thenThrow(new IllegalStateException("unknown type"));

        assertThat(service.isAllowed(5L, 1L, 7L)).isFalse();
    }
}
