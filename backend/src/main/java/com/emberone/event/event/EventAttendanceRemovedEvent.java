/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.event.event;

import com.emberone.event.domain.EventType;

/** Published when a user is removed from attendance of a service/operation event. */
public record EventAttendanceRemovedEvent(
        Long tenantId, EventType eventType, Long eventId, Long userId) {}
