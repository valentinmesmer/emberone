/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;
import java.time.Instant;

/**
 * Published when a service/operation event is created by promoting an appointment, so the
 * appointment module can validate and archive without {@code EventService} depending on it.
 * Listeners run synchronously in the creating transaction.
 */
public record EventPromotedFromAppointmentEvent(
        Long organizationId,
        Long appointmentId,
        EventType eventType,
        Instant eventStart,
        Instant eventEnd) {}
