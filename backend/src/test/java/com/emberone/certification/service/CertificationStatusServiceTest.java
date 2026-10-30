/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.tenant.TenantCertDefinition;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CertificationStatusServiceTest {

    @Test
    void computeStatusTreatsNullExpiryAsValid() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder().reminderDaysBeforeExpiry(30).build();
        UserCertificationRecord record =
                UserCertificationRecord.builder().completedDate(LocalDate.of(2020, 1, 1)).build();

        assertThat(
                        CertificationStatusService.computeStatus(
                                definition, record, LocalDate.of(2026, 7, 1)))
                .isEqualTo(CertificationStatus.VALID);
    }

    @Test
    void computeStatusMarksExpiredRecord() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder().reminderDaysBeforeExpiry(30).build();
        UserCertificationRecord record =
                UserCertificationRecord.builder()
                        .completedDate(LocalDate.of(2023, 1, 1))
                        .expiryDate(LocalDate.of(2025, 12, 31))
                        .build();

        assertThat(
                        CertificationStatusService.computeStatus(
                                definition, record, LocalDate.of(2026, 7, 1)))
                .isEqualTo(CertificationStatus.EXPIRED);
    }

    @Test
    void computeStatusMarksExpiringSoonWithinReminderWindow() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder().reminderDaysBeforeExpiry(30).build();
        UserCertificationRecord record =
                UserCertificationRecord.builder()
                        .completedDate(LocalDate.of(2023, 1, 1))
                        .expiryDate(LocalDate.of(2026, 7, 15))
                        .build();

        assertThat(
                        CertificationStatusService.computeStatus(
                                definition, record, LocalDate.of(2026, 7, 1)))
                .isEqualTo(CertificationStatus.EXPIRING_SOON);
    }

    @Test
    void computeStatusMarksMissingAsInReviewWhenSubmissionIsOpen() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder().reminderDaysBeforeExpiry(30).build();

        assertThat(
                        CertificationStatusService.computeStatus(
                                definition, null, LocalDate.of(2026, 7, 1), true))
                .isEqualTo(CertificationStatus.IN_REVIEW);
    }
}
