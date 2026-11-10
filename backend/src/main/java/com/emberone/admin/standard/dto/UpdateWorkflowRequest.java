/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Update the per-standard workflow approval settings: the enabled flag and the settings of every
 * approver slot role of the workflow type (all roles required, unknown roles are rejected).
 */
@Data
public class UpdateWorkflowRequest {

    private boolean enabled;

    @NotNull(message = "slots is required") @Valid private List<UpdateWorkflowSlotRequest> slots;
}
