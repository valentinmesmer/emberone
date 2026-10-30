/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

/** Concrete RSVP value on an appointment (org list filter). */
public enum AppointmentListResponseStatus {
    YES,
    NO,
    UNCERTAIN,
    /** Response expected, but the user has not answered yet. */
    NO_RESPONSE
}
