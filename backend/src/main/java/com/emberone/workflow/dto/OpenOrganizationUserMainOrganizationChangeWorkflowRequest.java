/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body for opening an org user-management main organization change workflow. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenOrganizationUserMainOrganizationChangeWorkflowRequest {

    @NotNull private Long newMainOrganizationId;

    @Size(max = OrganizationUserMainOrganizationChangeWorkflowPayload.MAX_REASON_LENGTH) private String reason;
}
