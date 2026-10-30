/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Whether the current user may open another workflow of a type and which concurrency keys are
 * taken.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowOpenStateDto {
    private long workflowTypeId;

    /**
     * True when {@code allowsMultipleConcurrentOpen} is false and an active instance already
     * exists.
     */
    private boolean typeBlocked;

    /**
     * Active concurrency keys for this workflow type (empty when only type-level blocking applies).
     */
    private List<String> openConcurrencyKeys;
}
