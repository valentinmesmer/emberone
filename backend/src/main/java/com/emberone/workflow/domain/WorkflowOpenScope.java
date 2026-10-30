/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

/**
 * Who may start a workflow type: the affected member ({@link #SELF}) or org user management with
 * {@code USER_WRITE} ({@link #ORGANIZATION}).
 */
public enum WorkflowOpenScope {
    SELF,
    ORGANIZATION
}
