/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.domain;

public enum TodoType {
    /** Group to-do: unclaimed organization-group approver slot - claim the workflow. */
    WORKFLOW_START_REQUESTED,
    /** User to-do: personal approval decision on a claimed or user-assigned slot. */
    WORKFLOW_APPROVAL_REQUIRED,
    /** User to-do: opener must update workflow input after an approver change request. */
    WORKFLOW_INPUT_CHANGE_REQUIRED,
    /**
     * User to-do: a required certification is missing, expired, or expiring soon - submit a fresh
     * proof.
     */
    CERTIFICATION_EXPIRING
}
