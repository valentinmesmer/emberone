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

/** The caller's active registration on a self-service offering, if any. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfCourseOfferingMyRegistrationDto {
    private Long id;
    private CourseRegistrationStatus status;
    private Integer waitlistPosition;

    @OrganizationWallClock private Instant registeredAt;

    private CourseRegistrationEndedBy endedBy;
    private String endReason;
    private Long workflowId;
    private Long cancellationWorkflowId;
    private boolean canCancel;
    private boolean cancellationPending;
    private boolean canWithdraw;
}
