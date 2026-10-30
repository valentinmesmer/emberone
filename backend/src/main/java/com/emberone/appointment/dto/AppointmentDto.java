/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.dto;

import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentType;
import com.emberone.appointment.domain.AudienceScope;
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
public class AppointmentDto {
    private Long id;
    private Long organizationId;
    private Long seriesId;
    private String recurrenceRule;

    @OrganizationWallClock private Instant seriesEnd;

    private String recurrenceTimezone;

    @OrganizationWallClock private Instant occurrenceStart;

    private String name;
    private AppointmentType type;

    @OrganizationWallClock private Instant start;

    @OrganizationWallClock private Instant end;
    private String location;
    private String description;
    private AppointmentResponseMode responseMode;
    private Integer responseDeadlineMinutesBefore;

    private AudienceScope audienceScope;
    private List<Long> audienceGroupIds;
    private List<AppointmentAudienceGroupDto> audienceGroups;
    private Boolean isException;

    private Boolean archived;

    /** True when the current user has not yet marked this appointment as read. */
    private Boolean unread;
}
