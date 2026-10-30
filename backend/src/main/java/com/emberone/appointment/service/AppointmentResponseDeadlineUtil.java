/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponseMode;
import java.time.Duration;
import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AppointmentResponseDeadlineUtil {

    public static int normalizeMinutes(AppointmentResponseMode mode, Integer minutesBefore) {
        if (mode == AppointmentResponseMode.NONE) {
            return 0;
        }
        return minutesBefore != null ? minutesBefore : 0;
    }

    public static boolean hasDeadline(Integer minutesBefore) {
        return minutesBefore != null && minutesBefore > 0;
    }

    private static Instant resolveDeadline(Instant start, Integer minutesBefore) {
        if (!hasDeadline(minutesBefore)) {
            return null;
        }
        return start.minus(Duration.ofMinutes(minutesBefore));
    }

    public static Instant resolveDeadline(Appointment appointment) {
        return resolveDeadline(
                appointment.getStart(), appointment.getResponseDeadlineMinutesBefore());
    }

    public static boolean isDeadlinePassed(Appointment appointment) {
        Instant deadline = resolveDeadline(appointment);
        return deadline != null && Instant.now().isAfter(deadline);
    }
}
