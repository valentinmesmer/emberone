/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.dto;

import com.emberone.courseoffering.domain.CourseOfferingStatus;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Completion list of an offering: manual final result per participant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCompletionDto {
    private Long offeringId;
    private CourseOfferingStatus status;

    /** Set once the list was confirmed; the list is then final. */
    private Instant completedAt;

    private List<Participant> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant {
        private Long registrationId;
        private Long userId;
        private String userDisplayName;

        /**
         * {@code true}: passed and credited on completion; {@code false}: not passed; null: open.
         */
        private Boolean finalPassed;

        private Long decidedByUserId;
        private Instant decidedAt;
    }
}
