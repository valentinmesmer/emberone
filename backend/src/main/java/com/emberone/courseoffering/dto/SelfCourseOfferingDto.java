/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An offering as seen by a user who may register for it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfCourseOfferingDto {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long trainingCourseTypeId;
    private String trainingCourseTypeName;
    private String content;
    private String notes;

    @OrganizationWallClock private Instant registrationStart;

    @OrganizationWallClock private Instant registrationEnd;

    private boolean registrationOpen;

    /**
     * All seats are taken. Registration stays possible; the user then lands on the waitlist with no
     * guaranteed seat.
     */
    private boolean full;

    /** The caller meets all prerequisites of the offering. */
    private boolean prerequisitesMet;

    private int maxParticipants;
    private boolean hasExam;
    private boolean requiresHomeOrgApproval;

    /** The user already holds the course this offering grants. */
    private boolean alreadyHeld;

    @OrganizationWallClock private Instant firstSessionStart;

    @OrganizationWallClock private Instant lastSessionEnd;

    private List<CourseSessionDto> sessions;

    /**
     * Active registration of the caller on this offering (`PENDING`, `PARTICIPANT`, `WAITLIST`).
     */
    private SelfCourseOfferingMyRegistrationDto myRegistration;
}
