/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.dto;

import com.emberone.platform.time.OrganizationWallClock;
import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.ParticipantSlotStatus;
import com.emberone.workflow.domain.PrincipalType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowParticipantDto {
    private Long id;
    private ParticipantRole role;
    private PrincipalType principalType;
    private Long principalId;
    private Long organizationId;

    /**
     * The approver slot role the organization decides in (e.g. {@code CONTEXT}, {@code ADDITIONAL},
     * {@code NEW_MAIN_ORG}); {@code CONTEXT} for user principals.
     */
    private String slotRole;

    /** Resolved from the settings of the slot's role in the organization (not stored). */
    private boolean selfApproveAllowed;

    /** Resolved organization name for org-bound approver slots. */
    private String organizationDisplayName;

    private ParticipantSlotStatus slotStatus;
    private Long claimedByUserId;
    @OrganizationWallClock private Instant claimedAt;

    /** Resolved group name or user display name for the slot principal. */
    private String principalDisplayName;

    /** Display name of the user who claimed this group slot, if any. */
    private String claimedByDisplayName;
}
