/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
