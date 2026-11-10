/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Approval settings of one approver slot role within {@link UpdateWorkflowRequest}. */
@Data
public class UpdateWorkflowSlotRequest {

    @NotBlank(message = "slotRole is required") private String slotRole;

    /** Approver organization group config key (e.g. LEADERSHIP). */
    @NotBlank(message = "approverOrgGroupConfigKey is required") private String approverOrgGroupConfigKey;

    private boolean autoApprove;
    private boolean selfApproveAllowed;
}
