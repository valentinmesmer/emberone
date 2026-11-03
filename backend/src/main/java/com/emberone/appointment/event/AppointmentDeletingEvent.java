/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.event;

/**
 * Published while an appointment is still present, immediately before the row is removed. Listeners
 * run synchronously in the deleting transaction so polymorphic attachment cleanup rolls back
 * together with the appointment delete if anything fails.
 */
public record AppointmentDeletingEvent(Long tenantId, Long organizationId, Long appointmentId) {}
