/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.event;

/**
 * Optional context for workflow lifecycle transitions that involve a participant and/or acting user
 * (claim, decision, withdraw, …).
 */
public record WorkflowEventContext(
        Long participantId, Long actorUserId, Long previousClaimerUserId) {

    public static WorkflowEventContext empty() {
        return new WorkflowEventContext(null, null, null);
    }

    public static WorkflowEventContext ofActor(Long participantId, Long actorUserId) {
        return new WorkflowEventContext(participantId, actorUserId, null);
    }

    public static WorkflowEventContext unclaimed(Long participantId, Long previousClaimerUserId) {
        return new WorkflowEventContext(
                participantId, previousClaimerUserId, previousClaimerUserId);
    }
}
