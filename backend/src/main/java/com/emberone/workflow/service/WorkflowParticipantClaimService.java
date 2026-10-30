/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.event.WorkflowEventContext;
import com.emberone.workflow.event.WorkflowEventPublisher;
import com.emberone.workflow.event.WorkflowLifecycleEvent;
import com.emberone.workflow.exception.WorkflowNotFoundException;
import com.emberone.workflow.repository.WorkflowParticipantRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowParticipantClaimService {

    private final WorkflowParticipantRepository participantRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowEventPublisher eventPublisher;

    /** Clears a pending group-approver claim and restores the normal group-claim todo lifecycle. */
    @Transactional
    public void releasePendingClaim(WorkflowParticipant participant, Long previousClaimerUserId) {
        Long workflowId = participant.getWorkflow().getId();
        participant.setClaimedByUserId(null);
        participant.setClaimedAt(null);
        participantRepository.save(participant);

        Workflow updated =
                workflowRepository
                        .findWithDetailsById(workflowId)
                        .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        eventPublisher.publish(
                updated,
                WorkflowLifecycleEvent.UNCLAIMED,
                WorkflowEventContext.unclaimed(participant.getId(), previousClaimerUserId));
    }
}
