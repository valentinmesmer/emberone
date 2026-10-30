/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.event;

public enum WorkflowLifecycleEvent {
    OPENED,
    APPROVED,
    REJECTED,
    CANCELLED,
    CLAIMED,
    UNCLAIMED,
    DECISION_RECORDED,
    APPROVAL_WITHDRAWN,
    /**
     * Pending approver slot dropped during configuration reconcile (todos for that slot are
     * cancelled).
     */
    APPROVER_SLOT_REMOVED,
    /** Approver asked the opener to change the workflow input. */
    INPUT_CHANGE_REQUESTED,
    /** Approver withdrew their open input-change request; workflow returns to OPEN. */
    INPUT_CHANGE_WITHDRAWN,
    /** Opener updated the workflow payload (free edit or after a change request). */
    INPUT_UPDATED,
    /** Human decisions cleared after an input update; claims retained. */
    DECISIONS_RESET,
    UPDATED,
    COMMENT_CREATED
}
