/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.*;
import lombok.experimental.UtilityClass;

/** Shared rules for when a workflow has been "taken over" and when the opener may edit input. */
@UtilityClass
public class WorkflowInputChangeRules {

    /**
     * Taken over when an organization-group approver slot is claimed, or a human decision exists.
     * USER auto-claims and system auto-approve decisions ({@code userId == null}) do not count.
     */
    private static boolean isTakenOver(Workflow workflow) {
        if (workflow == null) {
            return false;
        }
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (participant.isApprover()
                    && participant.getPrincipalType() == PrincipalType.ORGANIZATION_GROUP
                    && participant.getClaimedByUserId() != null) {
                return true;
            }
        }
        for (WorkflowDecision decision : workflow.getDecisions()) {
            if (decision.getUserId() != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean canEditInput(Workflow workflow, Long userId) {
        if (workflow == null || userId == null || !userId.equals(workflow.getOpenedByUserId())) {
            return false;
        }
        if (workflow.getStatus() == WorkflowStatus.AWAITING_INPUT_CHANGE) {
            return true;
        }
        return workflow.getStatus() == WorkflowStatus.OPEN && !isTakenOver(workflow);
    }

    public static boolean canRequestInputChange(Workflow workflow, Long userId) {
        if (workflow == null || userId == null || workflow.getStatus() != WorkflowStatus.OPEN) {
            return false;
        }
        if (!isTakenOver(workflow)) {
            return false;
        }
        for (WorkflowParticipant participant : workflow.getParticipants()) {
            if (participant.isApprover() && userId.equals(participant.getClaimedByUserId())) {
                return true;
            }
        }
        return false;
    }

    /** Only the user who opened the current input-change request may withdraw it. */
    public static boolean canWithdrawInputChange(Workflow workflow, Long userId) {
        if (workflow == null
                || userId == null
                || workflow.getStatus() != WorkflowStatus.AWAITING_INPUT_CHANGE) {
            return false;
        }
        return workflow.findOpenInputChange()
                .map(change -> userId.equals(change.getRequestedByUserId()))
                .orElse(false);
    }
}
