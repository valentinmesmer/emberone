/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.platform.time.OrganizationWallClock;
import com.emberone.workflow.domain.DecisionType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDecisionDto {
    private Long id;
    private Long participantId;
    private Long userId;

    /** Resolved display name when {@link #userId} is set. */
    private String userDisplayName;

    private DecisionType decisionType;
    private String comment;
    @OrganizationWallClock private Instant decidedAt;
}
