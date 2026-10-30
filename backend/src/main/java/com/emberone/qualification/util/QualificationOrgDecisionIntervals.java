/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.util;

import com.emberone.qualification.domain.QualificationOrgDecisionStatus;
import com.emberone.qualification.domain.UserQualificationOrgDecision;
import java.time.LocalDate;
import java.util.List;

public final class QualificationOrgDecisionIntervals {

    private QualificationOrgDecisionIntervals() {}

    private static boolean dateRangesOverlap(
            LocalDate existingStart, LocalDate existingEnd, LocalDate newStart, LocalDate newEnd) {
        LocalDate effectiveExistingEnd = existingEnd != null ? existingEnd : LocalDate.MAX;
        LocalDate effectiveNewEnd = newEnd != null ? newEnd : LocalDate.MAX;

        return (existingStart.isBefore(effectiveNewEnd) || existingStart.isEqual(effectiveNewEnd))
                && (effectiveExistingEnd.isAfter(newStart)
                        || effectiveExistingEnd.isEqual(newStart));
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
    }

    public static boolean hasOverlappingGrantedInterval(
            List<UserQualificationOrgDecision> existing,
            LocalDate newStart,
            LocalDate newEnd,
            Long excludeDecisionId) {
        return existing.stream()
                .filter(decision -> decision.getStatus() == QualificationOrgDecisionStatus.GRANTED)
                .filter(
                        decision ->
                                excludeDecisionId == null
                                        || !excludeDecisionId.equals(decision.getId()))
                .anyMatch(
                        decision ->
                                dateRangesOverlap(
                                        decision.getStartDate(),
                                        decision.getEndDate(),
                                        newStart,
                                        newEnd));
    }

    public static boolean isRevokedCoveringDate(
            UserQualificationOrgDecision decision, LocalDate date) {
        if (decision.getStatus() != QualificationOrgDecisionStatus.REVOKED) {
            return false;
        }
        if (decision.getStartDate().isAfter(date)) {
            return false;
        }
        return decision.getEndDate() == null || !decision.getEndDate().isBefore(date);
    }
}
