/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.domain.WorkflowTypeMappingSlot;
import com.emberone.workflow.spi.WorkflowApproverSlotRoles;
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

    @Mock WorkflowTypeMappingService mappingService;
    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock WorkflowHandler<Object> handler;
    @InjectMocks WorkflowSelfApproveService service;

    @Test
    void readsTheSettingOfTheSlotRoleInTheSlotOrganization() {
        Workflow workflow = workflow();
        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.CONTEXT))
                .thenReturn(slotSetting(true));
        when(mappingService.requireSlot(20L, 5L, WorkflowApproverSlotRoles.CONTEXT))
                .thenReturn(slotSetting(false));
        when(mappingService.requireSlot(20L, 5L, WorkflowApproverSlotRoles.NEW_MAIN_ORG))
                .thenReturn(slotSetting(true));

        assertThat(
                        service.isAllowed(
                                workflow, slot(1L, 10L, null, WorkflowApproverSlotRoles.CONTEXT)))
                .isTrue();
        assertThat(
                        service.isAllowed(
                                workflow, slot(2L, 20L, null, WorkflowApproverSlotRoles.CONTEXT)))
                .isFalse();
        assertThat(
                        service.isAllowed(
                                workflow,
                                slot(3L, 20L, null, WorkflowApproverSlotRoles.NEW_MAIN_ORG)))
                .isTrue();
        verifyNoInteractions(handlerRegistry);
    }

    @Test
    void slotWithoutOrganizationFallsBackToInstanceOrganization() {
        Workflow workflow = workflow();
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveInstanceOrganization(workflow)).thenReturn(10L);
        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.CONTEXT))
                .thenReturn(slotSetting(true));

        assertThat(
                        service.isAllowed(
                                workflow, slot(1L, null, null, WorkflowApproverSlotRoles.CONTEXT)))
                .isTrue();
        verify(handler, never()).resolveSecurityContextOrganization(any(), any());
    }

    @Test
    void missingSlotSettingFailsClosed() {
        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.CONTEXT))
                .thenThrow(new IllegalStateException("no mapping"));

        assertThat(
                        service.isAllowed(
                                workflow(), slot(1L, 10L, null, WorkflowApproverSlotRoles.CONTEXT)))
                .isFalse();
    }

    @Test
    void inputChangeRequestNeedsClaimedSlotAllowingSelfApprove() {
        WorkflowParticipant claimed = slot(1L, 10L, 1L, WorkflowApproverSlotRoles.NEW_MAIN_ORG);
        WorkflowParticipant other = slot(2L, 20L, 99L, WorkflowApproverSlotRoles.CONTEXT);
        Workflow workflow = workflow();
        workflow.getParticipants().addAll(Set.of(claimed, other));
        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.NEW_MAIN_ORG))
                .thenReturn(slotSetting(false));

        assertThat(service.blocksInputChangeRequest(workflow, 1L)).isTrue();
        assertThat(service.blocksInputChangeRequest(workflow, 99L)).isFalse();

        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.NEW_MAIN_ORG))
                .thenReturn(slotSetting(true));

        assertThat(service.blocksInputChangeRequest(workflow, 1L)).isFalse();
    }

    @Test
    void preOpenVariantUsesTheContextRole() {
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveSecurityContextOrganization(1L, 7L)).thenReturn(10L);
        when(mappingService.requireSlot(10L, 5L, WorkflowApproverSlotRoles.CONTEXT))
                .thenReturn(slotSetting(true));

        assertThat(service.isAllowed(5L, 1L, 7L)).isTrue();
    }

    @Test
    void missingContextOrganizationDisallowsSelfApprove() {
        doReturn(handler).when(handlerRegistry).require(5L);
        when(handler.resolveSecurityContextOrganization(1L, 7L)).thenReturn(null);

        assertThat(service.isAllowed(5L, 1L, 7L)).isFalse();
    }

    @Test
    void handlerOrMappingFailureFailsClosed() {
        when(handlerRegistry.require(5L)).thenThrow(new IllegalStateException("unknown type"));

        assertThat(service.isAllowed(5L, 1L, 7L)).isFalse();
    }

    private static WorkflowTypeMappingSlot slotSetting(boolean selfApproveAllowed) {
        return WorkflowTypeMappingSlot.builder().selfApproveAllowed(selfApproveAllowed).build();
    }

    private static Workflow workflow() {
        return Workflow.builder()
                .workflowTypeId(5L)
                .openedByUserId(1L)
                .tenantId(7L)
                .participants(new LinkedHashSet<>())
                .build();
    }

    private static WorkflowParticipant slot(
            Long id, Long organizationId, Long claimedBy, String slotRole) {
        return WorkflowParticipant.builder()
                .id(id)
                .role(ParticipantRole.APPROVER)
                .organizationId(organizationId)
                .claimedByUserId(claimedBy)
                .slotRole(slotRole)
                .build();
    }
}
