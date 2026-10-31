/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.domain;

/**
 * Lifecycle of a single input-change request on a workflow. Multiple requests may exist over time;
 * at most one may be {@link #OPEN}.
 */
public enum WorkflowInputChangeStatus {
    OPEN,
    FULFILLED,
    CANCELLED
}
