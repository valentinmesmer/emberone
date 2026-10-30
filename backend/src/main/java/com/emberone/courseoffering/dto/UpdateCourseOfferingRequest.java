/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.platform.time.OrganizationWallClock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class UpdateCourseOfferingRequest {

    private String content;

    private String notes;

    /** Required for templates, ignored otherwise. */
    @Size(max = 255, message = "Template name must be at most 255 characters") private String templateName;

    @OrganizationWallClock private Instant registrationStart;

    @OrganizationWallClock private Instant registrationEnd;

    @Min(value = 1, message = "Max participants must be at least 1") private int maxParticipants;

    private boolean hasExam;

    private boolean requiresHomeOrgApproval;

    /**
     * Sets the quotas of the existing audience entries together with the new maximum, so both can
     * change in one step. All entries must be listed (missing ones lose their quota; an empty list
     * removes all quotas). Null leaves the quotas as they are. Not allowed when creating.
     */
    @Valid private List<CourseAudienceQuotaRequest> audienceQuotas;
}
