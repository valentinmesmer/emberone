/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.platform.navigation.dto.AppLinkDto;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A registration as seen by the offering organization. */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourseRegistrationDto extends AppLinkDto {
    private Long id;
    private Long offeringId;
    private Long userId;

    /** The audience entry whose quota the place belongs to; null without quotas. */
    private Long audienceId;

    private String userDisplayName;
    private CourseRegistrationStatus status;

    /** 1-based place in the waitlist queue; set only while the status is {@code WAITLIST}. */
    private Integer waitlistPosition;

    /** The user already held the offered course when registering. */
    private boolean alreadyHeld;

    /** Registration request workflow; mirrored as AppLink when set. */
    private Long workflowId;

    private Instant registeredAt;
    private Instant approvedAt;
    private Instant endedAt;
    private CourseRegistrationEndedBy endedBy;
    private String endReason;
}
