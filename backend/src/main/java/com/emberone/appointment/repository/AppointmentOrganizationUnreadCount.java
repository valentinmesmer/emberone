/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.repository;

/** Per-organization unread appointment counts for self-service badges. */
public interface AppointmentOrganizationUnreadCount {

    Long getOrganizationId();

    long getUnreadCount();
}
