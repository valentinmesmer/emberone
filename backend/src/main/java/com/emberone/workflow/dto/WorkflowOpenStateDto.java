/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
     * Active concurrency keys for this workflow type (empty when only type-level blocking applies).
     */
    private List<String> openConcurrencyKeys;
}
