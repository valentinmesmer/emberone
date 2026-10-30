/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSessionDto {
    private Long id;
    private CourseSessionType type;

    @OrganizationWallClock private Instant start;

    @OrganizationWallClock private Instant end;

    private String location;
    private String notes;

    public static CourseSessionDto of(CourseSession session) {
        return CourseSessionDto.builder()
                .id(session.getId())
                .type(session.getType())
                .start(session.getStart())
                .end(session.getEnd())
                .location(session.getLocation())
                .notes(session.getNotes())
                .build();
    }
}
