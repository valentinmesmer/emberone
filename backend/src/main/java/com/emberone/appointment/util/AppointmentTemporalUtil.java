/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AppointmentTemporalUtil {

    /** Clears seconds and nanoseconds so appointment times align to whole minutes. */
    public static Instant truncateToMinutes(Instant dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.MINUTES);
    }
}
