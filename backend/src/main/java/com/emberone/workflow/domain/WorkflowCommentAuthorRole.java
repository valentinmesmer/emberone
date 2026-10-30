/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

/** Role of the author at the time a workflow discussion comment was posted. */
public enum WorkflowCommentAuthorRole {
    OPENER,
    APPROVER,
    VIEWER
}
