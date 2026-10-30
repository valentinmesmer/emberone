/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Seat usage of an offering. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRegistrationSummaryDto {
    private int maxParticipants;
    private long participants;
    private long waitlist;

    /** Registrations whose workflow is still open. */
    private long pending;

    private long freeSeats;

    /** Usage per audience entry; empty when the offering has no quotas. */
    @Builder.Default private List<Quota> quotas = List.of();

    /** Seat usage of one audience entry's quota. The waitlist is kept per entry. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quota {
        private Long audienceId;
        private CourseAudienceKind kind;
        private Long refId;
        private int quota;
        private long participants;
        private long waitlist;
        private long freeSeats;
    }
}
