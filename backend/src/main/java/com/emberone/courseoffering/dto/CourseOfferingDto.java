/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOfferingDto {
    private Long id;
    private Long organizationId;
    private Long trainingCourseTypeId;
    private String trainingCourseTypeName;
    private String content;
    private String notes;
    private String templateName;
    private CourseOfferingStatus status;

    @OrganizationWallClock private Instant registrationStart;

    @OrganizationWallClock private Instant registrationEnd;

    private int maxParticipants;
    private boolean hasExam;
    private boolean requiresHomeOrgApproval;

    /** Derived from the sessions; null while there are none. */
    @OrganizationWallClock private Instant firstSessionStart;

    @OrganizationWallClock private Instant lastSessionEnd;

    private List<CourseSessionDto> sessions;
    private List<CourseAudienceEntryDto> audiences;
    private CoursePrerequisitesDto prerequisites;

    /** Set while the offering is cancelled. */
    @OrganizationWallClock private Instant cancelledAt;

    private String cancelReason;
}
