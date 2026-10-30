/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.permission.domain.PermissionType;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.repository.WorkflowDecisionRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.WorkflowSelfApproveService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowInstanceAccessTest {

    private static final Long WORKFLOW_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long PARTICIPANT_ID = 30L;
    private static final Long ORGANIZATION_ID = 40L;
    private static final Long GROUP_ID = 50L;

    @Mock private WorkflowRepository workflowRepository;

    @Mock private WorkflowDecisionRepository decisionRepository;

    @Mock private UserOrganizationGroupMembershipRepository membershipRepository;

    @Mock private WorkflowSelfApproveService selfApproveService;

    @Mock private WorkflowHandlerRegistry workflowHandlerRegistry;

    @Mock private OrganizationAccessService organizationAccessService;

    @InjectMocks private WorkflowInstanceAccess access;

    private static Workflow workflow(
            WorkflowStatus status, Long openedByUserId, WorkflowParticipant... participants) {
        return Workflow.builder()
                .id(WORKFLOW_ID)
                .tenantId(1L)
                .workflowTypeId(2L)
                .status(status)
                .openedByUserId(openedByUserId)
                .participants(new LinkedHashSet<>(List.of(participants)))
                .build();
    }

    private static WorkflowParticipant participant(
            ParticipantRole role,
            PrincipalType principalType,
            Long principalId,
            Long claimedByUserId,
            ParticipantSlotStatus status) {
        return WorkflowParticipant.builder()
                .id(PARTICIPANT_ID)
                .role(role)
                .principalType(principalType)
                .principalId(principalId)
                .organizationId(ORGANIZATION_ID)
                .claimedByUserId(claimedByUserId)
                .slotStatus(status)
                .build();
    }

    @Test
    void canView_allowsOpenerDirectViewerAndGroupApprover() {
        Workflow openerWorkflow = workflow(WorkflowStatus.OPEN, USER_ID);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID))
                .thenReturn(Optional.of(openerWorkflow));
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isTrue();

        Workflow viewerWorkflow =
                workflow(
                        WorkflowStatus.OPEN,
                        99L,
                        participant(
                                ParticipantRole.VIEWER, PrincipalType.USER, USER_ID, null, null));
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID))
                .thenReturn(Optional.of(viewerWorkflow));
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isTrue();

        Workflow groupWorkflow =
                workflow(
                        WorkflowStatus.OPEN,
                        99L,
                        participant(
                                ParticipantRole.APPROVER,
                                PrincipalType.ORGANIZATION_GROUP,
                                GROUP_ID,
                                null,
                                ParticipantSlotStatus.PENDING));
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID))
                .thenReturn(Optional.of(groupWorkflow));
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(ORGANIZATION_ID),
                        any(Instant.class)))
                .thenReturn(List.of(GROUP_ID));
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isTrue();
    }

    @Test
    void canView_closedWorkflowAllowsPreviousDecisionButOpenWorkflowDoesNot() {
        Workflow closed = workflow(WorkflowStatus.APPROVED, 99L);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(closed));
        when(decisionRepository.existsByWorkflow_IdAndUserId(WORKFLOW_ID, USER_ID))
                .thenReturn(true);
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isTrue();

        Workflow open = workflow(WorkflowStatus.OPEN, 99L);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(open));
        stubNoWorkflowRead();
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isFalse();
    }

    @Test
    void canView_allowsOrganizationWorkflowReadOnOpenWorkflow() {
        Workflow open = workflow(WorkflowStatus.OPEN, 99L);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(open));
        @SuppressWarnings("unchecked")
        WorkflowHandler<Object> handler = org.mockito.Mockito.mock(WorkflowHandler.class);
        doReturn(handler).when(workflowHandlerRegistry).require(open.getWorkflowTypeId());
        when(handler.resolveInstanceOrganization(open)).thenReturn(ORGANIZATION_ID);
        when(organizationAccessService.hasOrganizationPermission(
                        ORGANIZATION_ID, PermissionType.WORKFLOW_READ))
                .thenReturn(true);

        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isTrue();
    }

    private void stubNoWorkflowRead() {
        @SuppressWarnings("unchecked")
        WorkflowHandler<Object> handler = org.mockito.Mockito.mock(WorkflowHandler.class);
        doReturn(handler).when(workflowHandlerRegistry).require(any());
        when(handler.resolveInstanceOrganization(any())).thenReturn(ORGANIZATION_ID);
        when(organizationAccessService.hasOrganizationPermission(
                        ORGANIZATION_ID, PermissionType.WORKFLOW_READ))
                .thenReturn(false);
    }

    @Test
    void canView_failsClosedForMissingUserWorkflowAndRepositoryError() {
        assertThat(access.canView(WORKFLOW_ID, null)).isFalse();
        verify(workflowRepository, never()).findWithDetailsById(WORKFLOW_ID);

        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.empty());
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isFalse();

        when(workflowRepository.findWithDetailsById(WORKFLOW_ID))
                .thenThrow(new IllegalStateException("db"));
        assertThat(access.canView(WORKFLOW_ID, USER_ID)).isFalse();
    }

    @Test
    void canDecide_requiresOpenPendingClaimedApproverAndHonorsSelfApproveRule() {
        WorkflowParticipant participant =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.USER,
                        USER_ID,
                        USER_ID,
                        ParticipantSlotStatus.PENDING);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, participant);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();
        // User-principal slots are not group-claim slots; unclaim is group-only.
        assertThat(access.canUnclaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();

        participant.setClaimedByUserId(77L);
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();

        participant.setClaimedByUserId(USER_ID);
        workflow.setOpenedByUserId(USER_ID);
        when(selfApproveService.isAllowed(workflow, ORGANIZATION_ID)).thenReturn(false);
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
    }

    @Test
    void canClaim_requiresUnclaimedGroupApproverMembershipAndNoSelfApproveBlock() {
        WorkflowParticipant participant =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.ORGANIZATION_GROUP,
                        GROUP_ID,
                        null,
                        ParticipantSlotStatus.PENDING);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, participant);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(ORGANIZATION_ID),
                        any(Instant.class)))
                .thenReturn(List.of(GROUP_ID));
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();

        participant.setClaimedByUserId(77L);
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();

        participant.setClaimedByUserId(null);
        workflow.setOpenedByUserId(USER_ID);
        when(selfApproveService.isAllowed(workflow, ORGANIZATION_ID)).thenReturn(false);
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
    }

    @Test
    void selfApproveIsDecidedBySlotOrganization_notByOtherSlots() {
        Long otherOrganizationId = 55L;
        Long otherParticipantId = 56L;
        Long otherGroupId = 57L;
        WorkflowParticipant allowed =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.ORGANIZATION_GROUP,
                        GROUP_ID,
                        null,
                        ParticipantSlotStatus.PENDING);
        WorkflowParticipant blocked =
                WorkflowParticipant.builder()
                        .id(otherParticipantId)
                        .role(ParticipantRole.APPROVER)
                        .principalType(PrincipalType.ORGANIZATION_GROUP)
                        .principalId(otherGroupId)
                        .organizationId(otherOrganizationId)
                        .slotStatus(ParticipantSlotStatus.PENDING)
                        .build();
        Workflow workflow = workflow(WorkflowStatus.OPEN, USER_ID, allowed, blocked);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        when(selfApproveService.isAllowed(workflow, ORGANIZATION_ID)).thenReturn(true);
        when(selfApproveService.isAllowed(workflow, otherOrganizationId)).thenReturn(false);
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(ORGANIZATION_ID),
                        any(Instant.class)))
                .thenReturn(List.of(GROUP_ID));
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(otherOrganizationId),
                        any(Instant.class)))
                .thenReturn(List.of(otherGroupId));

        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();
        assertThat(access.canClaim(WORKFLOW_ID, otherParticipantId, USER_ID)).isFalse();
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isTrue();
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isTrue();

        allowed.setClaimedByUserId(USER_ID);
        blocked.setClaimedByUserId(USER_ID);
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();
        assertThat(access.canDecide(WORKFLOW_ID, otherParticipantId, USER_ID)).isFalse();
        assertThat(access.canUnclaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();
        assertThat(access.canUnclaim(WORKFLOW_ID, otherParticipantId, USER_ID)).isFalse();
    }

    @Test
    void canWithdrawApproval_requiresMatchingApprovedDecision() {
        WorkflowParticipant participant =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.USER,
                        USER_ID,
                        USER_ID,
                        ParticipantSlotStatus.APPROVED);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, participant);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        when(decisionRepository.existsByWorkflow_IdAndParticipantIdAndUserIdAndDecisionType(
                        WORKFLOW_ID, PARTICIPANT_ID, USER_ID, DecisionType.APPROVED))
                .thenReturn(true);
        assertThat(access.canWithdrawApproval(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isTrue();

        participant.setSlotStatus(ParticipantSlotStatus.PENDING);
        assertThat(access.canWithdrawApproval(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
    }

    @Test
    void canUpdatePromotionsAndRequiresActionForRelatedPendingApprover() {
        WorkflowParticipant claimed =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.USER,
                        USER_ID,
                        USER_ID,
                        ParticipantSlotStatus.PENDING);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, claimed);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isTrue();
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isTrue();

        claimed.setSlotStatus(ParticipantSlotStatus.APPROVED);
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isFalse();

        workflow.setStatus(WorkflowStatus.APPROVED);
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isFalse();
    }

    @Test
    void requiresParticipantAction_supportsUnclaimedGroupMembership() {
        WorkflowParticipant group =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.ORGANIZATION_GROUP,
                        GROUP_ID,
                        null,
                        ParticipantSlotStatus.PENDING);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, group);
        when(membershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(ORGANIZATION_ID),
                        any(Instant.class)))
                .thenReturn(List.of(GROUP_ID));

        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isTrue();
        assertThat(access.requiresParticipantAction(null, USER_ID)).isFalse();
        assertThat(access.requiresParticipantAction(workflow, null)).isFalse();
    }

    @Test
    void canCommentRequiresOpenVisibleWorkflowAndCanCancelRequiresOpener() {
        Workflow workflow = workflow(WorkflowStatus.OPEN, USER_ID);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        assertThat(access.canComment(WORKFLOW_ID, USER_ID)).isTrue();
        assertThat(access.canCancel(WORKFLOW_ID, USER_ID)).isTrue();
        assertThat(access.canCancel(WORKFLOW_ID, 77L)).isFalse();

        workflow.setStatus(WorkflowStatus.CANCELLED);
        assertThat(access.canComment(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.canCancel(WORKFLOW_ID, USER_ID)).isFalse();
    }

    @Test
    void missingParticipantAndIncompleteGroupPrincipalFailClosed() {
        WorkflowParticipant group =
                participant(
                        ParticipantRole.APPROVER,
                        PrincipalType.ORGANIZATION_GROUP,
                        null,
                        null,
                        ParticipantSlotStatus.PENDING);
        Workflow workflow = workflow(WorkflowStatus.OPEN, 99L, group);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));

        assertThat(access.canDecide(WORKFLOW_ID, 999L, USER_ID)).isFalse();
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
    }

    @Test
    void permissionOperationsRejectMissingUsersAndUnavailableWorkflows() {
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, null)).isFalse();
        assertThat(access.canWithdrawApproval(WORKFLOW_ID, PARTICIPANT_ID, null)).isFalse();
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, null)).isFalse();
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, null)).isFalse();
        assertThat(access.canComment(WORKFLOW_ID, null)).isFalse();
        assertThat(access.canCancel(WORKFLOW_ID, null)).isFalse();

        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.empty());
        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
        assertThat(access.canWithdrawApproval(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
    }

    @Test
    void promotionAndActionChecksRejectSelfApprovalAndUnrelatedParticipants() {
        WorkflowParticipant viewer =
                participant(ParticipantRole.VIEWER, PrincipalType.USER, USER_ID, null, null);
        Workflow workflow = workflow(WorkflowStatus.OPEN, USER_ID, viewer);
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));

        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isFalse();

        workflow.setOpenedByUserId(99L);
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.requiresParticipantAction(workflow, USER_ID)).isFalse();
    }

    @Test
    void repositoryErrorsFailClosedAcrossMutatingPermissions() {
        when(workflowRepository.findWithDetailsById(WORKFLOW_ID))
                .thenThrow(new IllegalStateException("db"));

        assertThat(access.canDecide(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
        assertThat(access.canWithdrawApproval(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
        assertThat(access.canClaim(WORKFLOW_ID, PARTICIPANT_ID, USER_ID)).isFalse();
        assertThat(access.canUpdateDocumentPromotions(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.canComment(WORKFLOW_ID, USER_ID)).isFalse();
        assertThat(access.canCancel(WORKFLOW_ID, USER_ID)).isFalse();
    }
}
