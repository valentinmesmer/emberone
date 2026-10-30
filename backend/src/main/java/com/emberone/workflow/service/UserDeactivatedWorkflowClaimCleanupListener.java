/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.user.event.UserDeactivatedEvent;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Releases open workflow approver claims held by a deactivated user. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeactivatedWorkflowClaimCleanupListener {

    private final WorkflowParticipantRepository participantRepository;
    private final WorkflowParticipantClaimService claimService;

    @AfterCommitEventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        releaseClaimsForUser(event.userId());
    }

    @Transactional
    public void releaseClaimsForUser(Long userId) {
        if (userId == null) {
            return;
        }
        List<WorkflowParticipant> participants =
                participantRepository.findOpenPendingClaimsByUser(userId);
        for (WorkflowParticipant participant : participants) {
            claimService.releasePendingClaim(participant, userId);
            log.info(
                    "Released workflow claim for user {} on participant {} (workflow {})",
                    userId,
                    participant.getId(),
                    participant.getWorkflow().getId());
        }
    }
}
