/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;

/** Published when a user is marked as attending a service/operation event. */
public record EventAttendanceAddedEvent(
        Long tenantId, EventType eventType, Long eventId, Long userId) {}
