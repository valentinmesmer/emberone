/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Approval settings of one workflow type in a standard: enabled flag and one block per role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardWorkflowDto {

    private String configKey;
    private Long workflowTypeId;
    private boolean enabled;

    /** One entry per approver slot role of the type, in display order. */
    private List<StandardWorkflowSlotDto> slots;
}
