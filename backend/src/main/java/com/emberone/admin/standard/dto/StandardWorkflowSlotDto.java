/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Approver group and approval flags of one approver slot role. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardWorkflowSlotDto {

    /** Role key such as {@code CONTEXT}, {@code ADDITIONAL} or {@code NEW_MAIN_ORG}. */
    private String slotRole;

    /** Approver organization group config key (e.g. LEADERSHIP). */
    private String approverOrgGroupConfigKey;

    private boolean autoApprove;
    private boolean selfApproveAllowed;
}
