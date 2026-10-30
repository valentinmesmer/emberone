/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.workflow.domain.PrincipalType;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDecision;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowRecipientResolver {

    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final WorkflowHandlerRegistry handlerRegistry;

    public Set<Long> resolveRecipientUserIds(Workflow workflow) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (workflow.getOpenedByUserId() != null) {
            recipients.add(workflow.getOpenedByUserId());
        }
        if (workflow.getParticipants() != null) {
            for (WorkflowParticipant participant : workflow.getParticipants()) {
                addParticipantRecipients(recipients, participant);
            }
        }
        if (workflow.getDecisions() != null) {
            for (WorkflowDecision decision : workflow.getDecisions()) {
                if (decision.getUserId() != null) {
                    recipients.add(decision.getUserId());
                }
            }
        }
        return recipients;
    }

    public Long resolveOrganizationId(Workflow workflow) {
        if (workflow == null || workflow.getWorkflowTypeId() == null) {
            return null;
        }
        try {
            WorkflowHandler<?> handler = handlerRegistry.require(workflow.getWorkflowTypeId());
            return handler.resolveInstanceOrganization(workflow);
        } catch (Exception e) {
            return null;
        }
    }

    private void addParticipantRecipients(Set<Long> recipients, WorkflowParticipant participant) {
        if (participant.getPrincipalType() == PrincipalType.USER
                && participant.getPrincipalId() != null) {
            recipients.add(participant.getPrincipalId());
        }
        if (participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                && participant.getPrincipalId() != null) {
            recipients.addAll(
                    resolveActiveGroupMembers(
                            participant.getOrganizationId(), participant.getPrincipalId()));
        }
        if (participant.getClaimedByUserId() != null) {
            recipients.add(participant.getClaimedByUserId());
        }
    }

    private Set<Long> resolveActiveGroupMembers(Long organizationId, Long groupId) {
        Set<Long> members = new LinkedHashSet<>();
        if (groupId == null) {
            return members;
        }
        Instant now = Instant.now();
        for (UserOrganizationGroupMembership membership :
                membershipRepository.findByOrganizationGroup_IdWithUser(groupId)) {
            if (!membership.isActiveAt(now) || membership.getUser() == null) {
                continue;
            }
            if (organizationId != null
                    && membership.getOrganizationGroup() != null
                    && membership.getOrganizationGroup().getOrganization() != null
                    && !organizationId.equals(
                            membership.getOrganizationGroup().getOrganization().getId())) {
                continue;
            }
            members.add(membership.getUser().getId());
        }
        return members;
    }
}
