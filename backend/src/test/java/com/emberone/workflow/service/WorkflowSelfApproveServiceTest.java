/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.LinkedHashSet;
import java.util.Set;
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
    void resolvesCurrentDefinitionFromSlotOrganization() {
        Workflow workflow = workflow();
        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(true).build());
        when(definitionService.resolveEffectiveDefinition(5L, 20L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(false).build());

        assertThat(service.isAllowed(workflow, 10L)).isTrue();
        assertThat(service.isAllowed(workflow, 20L)).isFalse();
        verifyNoInteractions(handlerRegistry);
    }

    @Test
    void slotWithoutOrganizationFallsBackToInstanceOrganization() {
        Workflow workflow = workflow();
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveInstanceOrganization(workflow)).thenReturn(10L);
        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(true).build());

        assertThat(service.isAllowed(workflow, null)).isTrue();
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void missingSlotDefinitionFailsClosed() {
        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenThrow(new IllegalStateException("no definition"));

        assertThat(service.isAllowed(workflow(), 10L)).isFalse();
    }

    @Test
    void inputChangeRequestNeedsClaimedSlotAllowingSelfApprove() {
        WorkflowParticipant claimed = claimedSlot(1L, 10L, 1L);
        WorkflowParticipant other = claimedSlot(2L, 20L, 99L);
        Workflow workflow = workflow();
        workflow.getParticipants().addAll(Set.of(claimed, other));
        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(false).build());

        assertThat(service.blocksInputChangeRequest(workflow, 1L)).isTrue();
        assertThat(service.blocksInputChangeRequest(workflow, 99L)).isFalse();

        when(definitionService.resolveEffectiveDefinition(5L, 10L))
                .thenReturn(WorkflowDefinition.builder().selfApproveAllowed(true).build());

        assertThat(service.blocksInputChangeRequest(workflow, 1L)).isFalse();
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

    private static Workflow workflow() {
        return Workflow.builder()
                .workflowTypeId(5L)
                .openedByUserId(1L)
                .tenantId(7L)
                .participants(new LinkedHashSet<>())
                .build();
    }

    private static WorkflowParticipant claimedSlot(Long id, Long organizationId, Long claimedBy) {
        return WorkflowParticipant.builder()
                .id(id)
                .role(ParticipantRole.APPROVER)
                .organizationId(organizationId)
                .claimedByUserId(claimedBy)
                .build();
    }
}
