/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.platform.time.OrganizationWallClock;
import com.emberone.workflow.domain.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDto {
    private Long id;
    private Long workflowTypeId;
    private WorkflowStatus status;
    private Long openedByUserId;

    /** Resolved from {@link #openedByUserId} for list and detail responses. */
    private String openedByDisplayName;

    /** Resolved from the current admin workflow definition (not stored on the instance). */
    private boolean selfApproveAllowed;

    private String payload;

    /** Parsed from {@link com.emberone.workflow.domain.Workflow#getOpenConcurrencyKeys()}. */
    private List<String> openConcurrencyKeys;

    @OrganizationWallClock private Instant decidedAt;

    /** Currently open input-change request, if any ({@code status == OPEN}). */
    private WorkflowInputChangeDto activeInputChange;

    /** Full input-change history for this workflow (oldest first). */
    private List<WorkflowInputChangeDto> inputChanges;

    /**
     * True when the current user may update the workflow payload (free edit or fulfilling a change
     * request).
     */
    private boolean canEditInput;

    /** True when the current user may request an input change from the opener. */
    private boolean canRequestInputChange;

    /** True when the current user may withdraw the open input-change request. */
    private boolean canWithdrawInputChange;

    /**
     * True when a reject decision becomes an input-change request for this workflow type (comment
     * required) instead of terminating the workflow.
     */
    private boolean mapsRejectToInputChange;

    private List<WorkflowParticipantDto> participants;
    private List<WorkflowDecisionDto> decisions;
}
