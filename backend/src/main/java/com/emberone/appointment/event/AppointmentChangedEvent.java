/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.event;

/**
 * Published when an appointment is created, updated, or deleted so realtime subscribers can push
 * updates without coupling to {@code AppointmentService}.
 */
public record AppointmentChangedEvent(
        Long tenantId, Long organizationId, Long appointmentId, AppointmentChangeType changeType) {}
