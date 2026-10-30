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

/** One of the caller's own registrations, with what the caller may still do with it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfCourseRegistrationDto {
    private Long id;
    private Long offeringId;
    private Long trainingCourseTypeId;
    private String trainingCourseTypeName;
    private Long organizationId;
    private String organizationName;

    @OrganizationWallClock private Instant firstSessionStart;

    @OrganizationWallClock private Instant lastSessionEnd;

    private CourseRegistrationStatus status;

    /** 1-based place in the waitlist queue; set only while the status is {@code WAITLIST}. */
    private Integer waitlistPosition;

    private Instant registeredAt;
    private CourseRegistrationEndedBy endedBy;

    /** Reason given by the organization when it ended the registration. */
    private String endReason;

    /** The registration workflow. */
    private Long workflowId;

    /** The open or approved cancellation workflow, if any. */
    private Long cancellationWorkflowId;

    /** A participant may start a cancellation workflow now (before the first session). */
    private boolean canCancel;

    /** A cancellation workflow is open; the place stays until all deciders approved. */
    private boolean cancellationPending;

    /** A waitlist place may be withdrawn directly. */
    private boolean canWithdraw;
}
