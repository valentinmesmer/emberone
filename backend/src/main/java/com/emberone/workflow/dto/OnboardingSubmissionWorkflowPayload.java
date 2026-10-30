/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for {@link com.emberone.workflow.domain.WorkflowType#ONBOARDING_SUBMISSION}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSubmissionWorkflowPayload {

    @NotNull private Long invitationId;
}
