/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

public enum WorkflowStatus {
    OPEN,
    /**
     * Opener must update the payload once after an approver requested an input change. Still counts
     * as an active (non-terminal) workflow for concurrency and cancel rules.
     */
    AWAITING_INPUT_CHANGE,
    APPROVED,
    REJECTED,
    CANCELLED;

    /** Open or awaiting input - not yet terminal. */
    public boolean isActive() {
        return this == OPEN || this == AWAITING_INPUT_CHANGE;
    }
}
