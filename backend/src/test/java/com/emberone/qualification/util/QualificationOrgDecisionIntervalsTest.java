/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.qualification.domain.QualificationOrgDecisionStatus;
import com.emberone.qualification.domain.UserQualificationOrgDecision;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class QualificationOrgDecisionIntervalsTest {

    @Test
    void hasOverlappingGrantedInterval_detectsOpenEndedOverlap() {
        UserQualificationOrgDecision existing =
                UserQualificationOrgDecision.builder()
                        .id(1L)
                        .status(QualificationOrgDecisionStatus.GRANTED)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(null)
                        .build();
        assertThat(
                        QualificationOrgDecisionIntervals.hasOverlappingGrantedInterval(
                                List.of(existing), LocalDate.of(2026, 6, 1), null, null))
                .isTrue();
    }

    @Test
    void hasOverlappingGrantedInterval_ignoresAdjacentNonOverlappingRanges() {
        UserQualificationOrgDecision existing =
                UserQualificationOrgDecision.builder()
                        .id(1L)
                        .status(QualificationOrgDecisionStatus.GRANTED)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 3, 31))
                        .build();
        assertThat(
                        QualificationOrgDecisionIntervals.hasOverlappingGrantedInterval(
                                List.of(existing),
                                LocalDate.of(2026, 4, 1),
                                LocalDate.of(2026, 6, 30),
                                null))
                .isFalse();
    }

    @Test
    void validateDateRange_requiresStartDate() {
        assertThatThrownBy(() -> QualificationOrgDecisionIntervals.validateDateRange(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date");
    }

    @Test
    void hasOverlappingGrantedInterval_ignoresRevoked() {
        UserQualificationOrgDecision revoked =
                UserQualificationOrgDecision.builder()
                        .id(1L)
                        .status(QualificationOrgDecisionStatus.REVOKED)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(null)
                        .build();
        assertThat(
                        QualificationOrgDecisionIntervals.hasOverlappingGrantedInterval(
                                List.of(revoked), LocalDate.of(2026, 2, 1), null, null))
                .isFalse();
    }

    @Test
    void hasOverlappingGrantedInterval_detectsOverlapAmongGranted() {
        UserQualificationOrgDecision existing =
                UserQualificationOrgDecision.builder()
                        .id(1L)
                        .status(QualificationOrgDecisionStatus.GRANTED)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 12, 31))
                        .build();
        assertThat(
                        QualificationOrgDecisionIntervals.hasOverlappingGrantedInterval(
                                List.of(existing),
                                LocalDate.of(2026, 6, 1),
                                LocalDate.of(2026, 7, 1),
                                null))
                .isTrue();
    }
}
