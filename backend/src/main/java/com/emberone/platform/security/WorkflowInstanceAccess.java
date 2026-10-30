/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.permission.domain.PermissionType;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.workflow.domain.*;
import com.emberone.workflow.repository.WorkflowDecisionRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.WorkflowInputChangeRules;
import com.emberone.workflow.service.WorkflowSelfApproveService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-instance access rules (Abschnitt 13). All methods fail closed (return false on any resolution
 * error).
 */
@Component
@RequiredArgsConstructor
public class WorkflowInstanceAccess {

    private final WorkflowRepository workflowRepository;
    private final WorkflowDecisionRepository decisionRepository;
    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final WorkflowSelfApproveService selfApproveService;
    private final WorkflowHandlerRegistry workflowHandlerRegistry;
    private final OrganizationAccessService organizationAccessService;

    @Transactional(readOnly = true)
    public boolean canView(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null) {
                return false;
            }
            if (userId.equals(workflow.getOpenedByUserId())) {
                return true;
            }
            for (WorkflowParticipant p : workflow.getParticipants()) {
                if (p.getRole() == ParticipantRole.VIEWER
                        && p.getPrincipalType() == PrincipalType.USER
                        && userId.equals(p.getPrincipalId())) {
                    return true;
                }
            }
            for (WorkflowParticipant p : workflow.getParticipants()) {
                if (relatesToApproverSlot(p, userId)) {
                    return true;
                }
            }
            if (hasOrganizationWorkflowRead(workflow)) {
                return true;
            }
            if (workflow.getStatus() == WorkflowStatus.OPEN
                    || workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE) {
                return false;
            }
            return decisionRepository.existsByWorkflow_IdAndUserId(workflowId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canDecide(Long workflowId, Long participantId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || workflow.getStatus() != WorkflowStatus.OPEN) {
                return false;
            }
            WorkflowParticipant participant = findParticipant(workflow, participantId);
            return participant != null
                    && participant.isApprover()
                    && participant.getSlotStatus() == ParticipantSlotStatus.PENDING
                    && userId.equals(participant.getClaimedByUserId())
                    && !blocksSelfApprove(workflow, participant, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canUnclaim(Long workflowId, Long participantId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || !workflow.getStatus().isActive()) {
                return false;
            }
            WorkflowParticipant participant = findParticipant(workflow, participantId);
            return participant != null
                    && participant.isApprover()
                    && participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                    && participant.getSlotStatus() == ParticipantSlotStatus.PENDING
                    && userId.equals(participant.getClaimedByUserId())
                    && !blocksSelfApprove(workflow, participant, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canWithdrawApproval(Long workflowId, Long participantId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || workflow.getStatus() != WorkflowStatus.OPEN) {
                return false;
            }
            WorkflowParticipant participant = findParticipant(workflow, participantId);
            return participant != null
                    && participant.isApprover()
                    && participant.getSlotStatus() == ParticipantSlotStatus.APPROVED
                    && decisionRepository
                            .existsByWorkflow_IdAndParticipantIdAndUserIdAndDecisionType(
                                    workflowId, participantId, userId, DecisionType.APPROVED);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canClaim(Long workflowId, Long participantId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || workflow.getStatus() != WorkflowStatus.OPEN) {
                return false;
            }
            WorkflowParticipant participant = findParticipant(workflow, participantId);
            return participant != null
                    && participant.isApprover()
                    && participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                    && participant.getClaimedByUserId() == null
                    && isActiveGroupMember(
                            userId, participant.getOrganizationId(), participant.getPrincipalId())
                    && !blocksSelfApprove(workflow, participant, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canUpdateDocumentPromotions(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || workflow.getStatus() != WorkflowStatus.OPEN) {
                return false;
            }
            for (WorkflowParticipant participant : workflow.getParticipants()) {
                if (relatesToApproverSlot(participant, userId)
                        && !blocksSelfApprove(workflow, participant, userId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canComment(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            return workflow != null
                    && workflow.getStatus().isActive()
                    && canView(workflowId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canUpdatePayload(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null || !WorkflowInputChangeRules.canEditInput(workflow, userId)) {
                return false;
            }
            if (workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE) {
                return true;
            }
            WorkflowHandler<?> handler =
                    workflowHandlerRegistry.require(workflow.getWorkflowTypeId());
            return handler.allowsOpenerFreeInputEdit();
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canRequestInputChange(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            if (workflow == null) {
                return false;
            }
            WorkflowHandler<?> handler =
                    workflowHandlerRegistry.require(workflow.getWorkflowTypeId());
            if (!handler.allowsInputChangeRequests()) {
                return false;
            }
            return WorkflowInputChangeRules.canRequestInputChange(workflow, userId)
                    && !selfApproveService.blocksInputChangeRequest(workflow, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canWithdrawInputChange(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            return WorkflowInputChangeRules.canWithdrawInputChange(workflow, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * True when the user can claim or decide at least one pending approver slot on an open
     * workflow. Used to limit participation inbox lists to actionable items.
     */
    @Transactional(readOnly = true)
    public boolean requiresParticipantAction(Workflow workflow, Long userId) {
        if (userId == null || workflow == null || workflow.getStatus() != WorkflowStatus.OPEN) {
            return false;
        }
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (!participant.isApprover()
                    || participant.getSlotStatus() != ParticipantSlotStatus.PENDING
                    || blocksSelfApprove(workflow, participant, userId)) {
                continue;
            }
            if (userId.equals(participant.getClaimedByUserId())) {
                return true;
            }
            if (participant.getClaimedByUserId() == null
                    && participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                    && isActiveGroupMember(
                            userId,
                            participant.getOrganizationId(),
                            participant.getPrincipalId())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean canCancel(Long workflowId, Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            Workflow workflow = workflowRepository.findWithDetailsById(workflowId).orElse(null);
            return workflow != null
                    && workflow.getStatus().isActive()
                    && userId.equals(workflow.getOpenedByUserId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasOrganizationWorkflowRead(Workflow workflow) {
        try {
            WorkflowHandler<?> handler =
                    workflowHandlerRegistry.require(workflow.getWorkflowTypeId());
            Long organizationId = handler.resolveInstanceOrganization(workflow);
            return organizationId != null
                    && organizationAccessService.hasOrganizationPermission(
                            organizationId, PermissionType.WORKFLOW_READ);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean blocksSelfApprove(
            Workflow workflow, WorkflowParticipant participant, Long userId) {
        return userId.equals(workflow.getOpenedByUserId())
                && !selfApproveService.isAllowed(workflow, participant.getOrganizationId());
    }

    private boolean relatesToApproverSlot(WorkflowParticipant p, Long userId) {
        if (!p.isApprover()) {
            return false;
        }
        if (p.getPrincipalType() == PrincipalType.USER) {
            return userId.equals(p.getPrincipalId());
        }
        return isActiveGroupMember(userId, p.getOrganizationId(), p.getPrincipalId());
    }

    private boolean isActiveGroupMember(Long userId, Long organizationId, Long groupId) {
        if (groupId == null) {
            return false;
        }
        if (organizationId != null) {
            return membershipRepository
                    .findActiveGroupIdsForUserInOrganization(userId, organizationId, Instant.now())
                    .contains(groupId);
        }
        Instant now = Instant.now();
        return membershipRepository.findByUser_Id(userId).stream()
                .filter(
                        m ->
                                m.getOrganizationGroup() != null
                                        && groupId.equals(m.getOrganizationGroup().getId()))
                .anyMatch(m -> m.isActiveAt(now));
    }

    private WorkflowParticipant findParticipant(Workflow workflow, Long participantId) {
        if (participantId == null) {
            return null;
        }
        return workflow.getParticipants().stream()
                .filter(p -> participantId.equals(p.getId()))
                .findFirst()
                .orElse(null);
    }
}
