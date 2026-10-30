/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipDeactivatedEvent;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps workflow approver claims aligned with organization-group membership changes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGroupMembershipSyncService {

    private final WorkflowParticipantRepository participantRepository;
    private final WorkflowParticipantClaimService claimService;

    @AfterCommitEventListener
    public void onOrganizationGroupMembershipDeactivated(
            OrganizationGroupMembershipDeactivatedEvent event) {
        releaseClaimsForUserLeavingGroup(event.userId(), event.organizationGroupId());
    }

    /** Releases pending approver claims when a user is no longer an active member of the group. */
    @Transactional
    public void releaseClaimsForUserLeavingGroup(Long userId, Long organizationGroupId) {
        if (userId == null || organizationGroupId == null) {
            return;
        }
        List<WorkflowParticipant> participants =
                participantRepository.findOpenPendingClaimsByUserAndOrganizationGroup(
                        userId, organizationGroupId);
        for (WorkflowParticipant participant : participants) {
            claimService.releasePendingClaim(participant, userId);
            log.info(
                    "Released workflow claim for user {} on participant {} (workflow {}) after group membership ended",
                    userId,
                    participant.getId(),
                    participant.getWorkflow().getId());
        }
    }
}
