/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.dto;

import com.emberone.workflow.dto.OrganizationMembershipEndRequestWorkflowPayload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndUserOrganizationMembershipRequest {

    @NotBlank(message = "Reason is required") @Size(max = OrganizationMembershipEndRequestWorkflowPayload.MAX_REASON_LENGTH) private String reason;
}
