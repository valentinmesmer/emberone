/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.domain;

/** Role of the author at the time a workflow discussion comment was posted. */
public enum WorkflowCommentAuthorRole {
    OPENER,
    APPROVER,
    VIEWER
}
