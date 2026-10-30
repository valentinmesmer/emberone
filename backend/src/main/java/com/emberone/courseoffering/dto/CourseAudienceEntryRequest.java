/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseAudienceEntryRequest {

    @NotNull(message = "Kind is required") private CourseAudienceKind kind;

    /** Organization id (ORG, ORG_SUBTREE) or organization type id (ORG_TYPE); null otherwise. */
    private Long refId;

    /**
     * Seats reserved for this entry. Either every entry has one or none; the quotas must not add up
     * to more than the maximum number of participants. Not allowed for tenant-wide and
     * own-organization audiences.
     */
    @Min(value = 1, message = "Quota must be at least 1") private Integer quota;
}
