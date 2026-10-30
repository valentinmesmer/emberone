/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponseMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AppointmentResponseDeadlineUtilTest {

    @Test
    void normalizeMinutesReturnsZeroForNoneMode() {
        assertThat(
                        AppointmentResponseDeadlineUtil.normalizeMinutes(
                                AppointmentResponseMode.NONE, 30))
                .isZero();
    }

    @Test
    void normalizeMinutesDefaultsNullToZero() {
        assertThat(
                        AppointmentResponseDeadlineUtil.normalizeMinutes(
                                AppointmentResponseMode.YES_NO, null))
                .isZero();
    }

    @Test
    void resolveDeadlineUsesAppointmentFields() {
        Instant start = LocalDateTime.of(2026, 6, 12, 12, 0).toInstant(ZoneOffset.UTC);
        Appointment withMinutes =
                Appointment.builder().start(start).responseDeadlineMinutesBefore(30).build();
        assertThat(AppointmentResponseDeadlineUtil.resolveDeadline(withMinutes))
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 30).toInstant(ZoneOffset.UTC));

        Appointment noDeadline =
                Appointment.builder().start(start).responseDeadlineMinutesBefore(0).build();
        assertThat(AppointmentResponseDeadlineUtil.resolveDeadline(noDeadline)).isNull();

        Appointment nullMinutes =
                Appointment.builder().start(start).responseDeadlineMinutesBefore(null).build();
        assertThat(AppointmentResponseDeadlineUtil.resolveDeadline(nullMinutes)).isNull();

        Appointment hourBefore =
                Appointment.builder()
                        .start(LocalDateTime.of(2026, 6, 12, 18, 0).toInstant(ZoneOffset.UTC))
                        .responseDeadlineMinutesBefore(60)
                        .build();
        assertThat(AppointmentResponseDeadlineUtil.resolveDeadline(hourBefore))
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 17, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    void isDeadlinePassedWhenDeadlineIsInThePast() {
        Appointment appointment =
                Appointment.builder()
                        .start(Instant.now().plus(Duration.ofMinutes(5)))
                        .responseDeadlineMinutesBefore(10)
                        .build();

        assertThat(AppointmentResponseDeadlineUtil.isDeadlinePassed(appointment)).isTrue();
    }
}
