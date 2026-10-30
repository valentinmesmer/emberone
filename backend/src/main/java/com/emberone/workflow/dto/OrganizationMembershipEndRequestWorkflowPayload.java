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

/**
 * Payload for {@link
 * com.emberone.workflow.domain.WorkflowType#ORGANIZATION_MEMBERSHIP_END_REQUEST}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembershipEndRequestWorkflowPayload {

    public static final int MAX_REASON_LENGTH = 2000;

    @NotNull private Long organizationId;

    @NotNull private Long targetUserId;

    /** Snapshot at open time; set by the server. */
    @Size(max = 255) private String organizationName;

    /** Snapshot at open time; set by the server. */
    @Size(max = 255) private String targetUserDisplayName;

    @Size(max = MAX_REASON_LENGTH) private String reason;
}
