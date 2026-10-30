/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.event;

/**
 * Published while an appointment is still present, immediately before the row is removed. Listeners
 * run synchronously in the deleting transaction so polymorphic attachment cleanup rolls back
 * together with the appointment delete if anything fails.
 */
public record AppointmentDeletingEvent(Long tenantId, Long organizationId, Long appointmentId) {}
