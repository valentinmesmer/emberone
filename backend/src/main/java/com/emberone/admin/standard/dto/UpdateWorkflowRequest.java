/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Update per-standard workflow approval routing (approver group + auto-approve). */
@Data
public class UpdateWorkflowRequest {

    /** Approver organization group config key (e.g. LEADERSHIP). */
    @NotBlank(message = "approverOrgGroupConfigKey is required") private String approverOrgGroupConfigKey;

    private boolean autoApprove;
    private boolean enabled;
    private boolean selfApproveAllowed;
}
