/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.platform.time.OrganizationWallClock;
import com.emberone.workflow.domain.WorkflowInputChangeStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInputChangeDto {
    private Long id;
    @OrganizationWallClock private Instant requestedAt;
    private String message;
    private WorkflowInputChangeStatus status;
    @OrganizationWallClock private Instant fulfilledAt;
}
