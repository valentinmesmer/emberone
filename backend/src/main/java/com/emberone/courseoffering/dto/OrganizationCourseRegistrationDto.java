/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A registration in the organization-wide list: who registered for which course offering. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCourseRegistrationDto {
    private Long id;
    private Long offeringId;
    private Long userId;
    private String userDisplayName;
    private String trainingCourseTypeName;
    @OrganizationWallClock private Instant firstSessionStart;
    @OrganizationWallClock private Instant lastSessionEnd;
    private CourseRegistrationStatus status;
}
