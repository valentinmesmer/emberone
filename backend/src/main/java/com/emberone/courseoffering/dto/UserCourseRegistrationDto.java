/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A member's registration as shown in the member view of any organization: status information only,
 * no attendance or exam details of offerings run by other organizations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCourseRegistrationDto {
    private Long id;
    private Long offeringId;
    private Long trainingCourseTypeId;
    private String trainingCourseTypeName;
    private Long organizationId;
    private String organizationName;

    @OrganizationWallClock private Instant firstSessionStart;

    @OrganizationWallClock private Instant lastSessionEnd;

    private CourseRegistrationStatus status;
    private CourseRegistrationEndedBy endedBy;
}
