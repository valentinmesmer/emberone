/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The standard resolution for a single organization: whether it overrides its org-type default, the
 * type default itself, and the resulting effective standard. Drives the org-detail "Standard" card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStandardDto {
    private Long organizationId;
    private Long organizationTypeId;

    /**
     * True when an explicit per-org override is set; false when the organization inherits its type
     * default.
     */
    private boolean overridden;

    /** The explicit per-org override standard id; null when inheriting. */
    private Long overrideStandardId;

    /** Name of the standard assigned to the organization's type; null when none assigned. */
    private String typeDefaultStandardName;

    /**
     * The standard that effectively drives the organization (override when set, else type default).
     */
    private Long effectiveStandardId;

    private String effectiveStandardName;
}
