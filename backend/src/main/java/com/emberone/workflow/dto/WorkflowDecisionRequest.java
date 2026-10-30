/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import com.emberone.workflow.domain.DecisionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDecisionRequest {

    @NotNull(message = "participantId is required") private Long participantId;

    @NotNull(message = "decision is required") private DecisionType decision;

    @Size(max = 2000, message = "Comment must be at most 2000 characters") private String comment;
}
